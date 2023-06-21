package org.hibernate.tool.orm.jbt.wrp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Properties;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.DefaultNamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.tool.orm.jbt.util.MetadataHelper;
import org.hibernate.tool.orm.jbt.util.NativeConfiguration;
import org.hibernate.tool.orm.jbt.wrp.ConfigurationWrapperFactory.ConfigurationWrapper;
import org.hibernate.tool.orm.jbt.wrp.ConfigurationWrapperFactory.JpaConfigurationWrapperImpl;
import org.hibernate.tool.orm.jbt.wrp.ConfigurationWrapperFactory.RevengConfigurationWrapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.helpers.DefaultHandler;

public class ConfigurationWrapperFactoryTest {
	
	private static final String TEST_HBM_XML_STRING =
			"<hibernate-mapping package='org.jboss.tools.hibernate.orm.runtime.exp.internal'>" +
			"  <class name='ConfigurationWrapperFactoryTest$Foo'>" + 
			"    <id name='id'/>" +
			"  </class>" +
			"</hibernate-mapping>";
	
	private ConfigurationWrapper nativeConfigurationWrapper = null;
	private Configuration wrappedNativeConfiguration = null;
	private ConfigurationWrapper revengConfigurationWrapper = null;
	private Configuration wrappedRevengConfiguration = null;
	private ConfigurationWrapper jpaConfigurationWrapper = null;
	private Configuration wrappedJpaConfiguration = null;
	
	@BeforeEach
	public void beforeEach() {
		nativeConfigurationWrapper = ConfigurationWrapperFactory.createNativeConfigurationWrapper();
		wrappedNativeConfiguration = nativeConfigurationWrapper.getWrappedObject();
		revengConfigurationWrapper = ConfigurationWrapperFactory.createRevengConfigurationWrapper();
		wrappedRevengConfiguration = revengConfigurationWrapper.getWrappedObject();
		jpaConfigurationWrapper = ConfigurationWrapperFactory.createJpaConfigurationWrapper(null, null);
		wrappedJpaConfiguration = jpaConfigurationWrapper.getWrappedObject();
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(nativeConfigurationWrapper);
		assertNotNull(wrappedNativeConfiguration);
		assertNotNull(revengConfigurationWrapper);
		assertNotNull(wrappedRevengConfiguration);
		assertNotNull(jpaConfigurationWrapper);
		assertNotNull(wrappedJpaConfiguration);
	}
	
	@Test
	public void testGetProperty() {
		assertNull(nativeConfigurationWrapper.getProperty("foo"));
		wrappedNativeConfiguration.setProperty("foo", "bar");
		assertEquals("bar", nativeConfigurationWrapper.getProperty("foo"));
		assertNull(revengConfigurationWrapper.getProperty("foo"));
		wrappedRevengConfiguration.setProperty("foo", "bar");
		assertEquals("bar", revengConfigurationWrapper.getProperty("foo"));
		assertNull(jpaConfigurationWrapper.getProperty("foo"));
		wrappedJpaConfiguration.setProperty("foo", "bar");
		assertEquals("bar", jpaConfigurationWrapper.getProperty("foo"));
	}
	
	@Test
	public void testAddFile() throws Exception {
		File testFile = File.createTempFile("test", "hbm.xml");
		PrintWriter printWriter = new PrintWriter(testFile);
		printWriter.write(TEST_HBM_XML_STRING);
		printWriter.close();
		testFile.deleteOnExit();
		// For native configuration
		MetadataSources metadataSources = MetadataHelper.getMetadataSources(wrappedNativeConfiguration);
		assertTrue(metadataSources.getXmlBindings().isEmpty());
		assertSame(
				wrappedNativeConfiguration,
				nativeConfigurationWrapper.addFile(testFile));
		assertFalse(metadataSources.getXmlBindings().isEmpty());
		Binding<?> binding = metadataSources.getXmlBindings().iterator().next();
		assertEquals(testFile.getAbsolutePath(), binding.getOrigin().getName());
		// For reveng configuration
		try {
			revengConfigurationWrapper.addFile(testFile);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'addFile' should not be called on instances of " + RevengConfigurationWrapperImpl.class.getName());
		}
		// For jpa configuration
		try {
			jpaConfigurationWrapper.addFile(testFile);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'addFile' should not be called on instances of " + JpaConfigurationWrapperImpl.class.getName());
		}
	}
	
	@Test 
	public void testSetProperty() {
		// For native configuration
		assertNull(wrappedNativeConfiguration.getProperty("foo"));
		nativeConfigurationWrapper.setProperty("foo", "bar");
		assertEquals("bar", wrappedNativeConfiguration.getProperty("foo"));
		// For reveng configuration
		assertNull(wrappedRevengConfiguration.getProperty("foo"));
		revengConfigurationWrapper.setProperty("foo", "bar");
		assertEquals("bar", wrappedRevengConfiguration.getProperty("foo"));
		// For jpa configuration
		assertNull(wrappedJpaConfiguration.getProperty("foo"));
		jpaConfigurationWrapper.setProperty("foo", "bar");
		assertEquals("bar", wrappedJpaConfiguration.getProperty("foo"));
	}

	@Test 
	public void testSetProperties() {
		Properties testProperties = new Properties();
		// For native configuration
		assertNotSame(testProperties, wrappedNativeConfiguration.getProperties());
		assertSame(
				wrappedNativeConfiguration, 
				nativeConfigurationWrapper.setProperties(testProperties));
		assertSame(testProperties, wrappedNativeConfiguration.getProperties());
		// For reveng configuration
		assertNotSame(testProperties, wrappedRevengConfiguration.getProperties());
		assertSame(
				wrappedRevengConfiguration, 
				revengConfigurationWrapper.setProperties(testProperties));
		assertSame(testProperties, wrappedRevengConfiguration.getProperties());
		// For jpa configuration
		assertNotSame(testProperties, wrappedJpaConfiguration.getProperties());
		assertSame(
				wrappedJpaConfiguration, 
				jpaConfigurationWrapper.setProperties(testProperties));
		assertSame(testProperties, wrappedJpaConfiguration.getProperties());
	}
	
	@Test
	public void testSetEntityResolver() throws Exception {
		EntityResolver testResolver = new DefaultHandler();
		// For native configuration
		Field entityResolverField = NativeConfiguration.class.getDeclaredField("entityResolver");
		entityResolverField.setAccessible(true);
		assertNull(entityResolverField.get(wrappedNativeConfiguration));
		nativeConfigurationWrapper.setEntityResolver(testResolver);
		assertNotNull(entityResolverField.get(wrappedNativeConfiguration));
		assertSame(testResolver, entityResolverField.get(wrappedNativeConfiguration));
		// For reveng configuration
		try {
			revengConfigurationWrapper.setEntityResolver(testResolver);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setEntityResolver' should not be called on instances of " + RevengConfigurationWrapperImpl.class.getName());
		}
		// For jpa configuration
		try {
			jpaConfigurationWrapper.setEntityResolver(testResolver);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setEntityResolver' should not be called on instances of " + JpaConfigurationWrapperImpl.class.getName());
		}
	}
	
	@Test
	public void testSetNamingStrategy() throws Exception {
		NamingStrategy namingStrategy = new DefaultNamingStrategy();
		// For native configuration
		Field namingStrategyField = NativeConfiguration.class.getDeclaredField("namingStrategy");
		namingStrategyField.setAccessible(true);
		assertNull(namingStrategyField.get(wrappedNativeConfiguration));
		nativeConfigurationWrapper.setNamingStrategy(namingStrategy);
		assertNotNull(namingStrategyField.get(wrappedNativeConfiguration));
		assertSame(namingStrategyField.get(wrappedNativeConfiguration), namingStrategy);
		// For reveng configuration
		try {
			revengConfigurationWrapper.setNamingStrategy(namingStrategy);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setNamingStrategy' should not be called on instances of " + RevengConfigurationWrapperImpl.class.getName());
		}
		// For jpa configuration
		try {
			jpaConfigurationWrapper.setNamingStrategy(namingStrategy);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setNamingStrategy' should not be called on instances of " + JpaConfigurationWrapperImpl.class.getName());
		}
	}
	
	@Test
	public void testGetProperties() {
		Properties testProperties = new Properties();
		// For native configuration
		assertNotSame(testProperties, nativeConfigurationWrapper.getProperties());
		wrappedNativeConfiguration.setProperties(testProperties);
		assertSame(testProperties, nativeConfigurationWrapper.getProperties());
		// For reveng configuration
		assertNotSame(testProperties, revengConfigurationWrapper.getProperties());
		wrappedRevengConfiguration.setProperties(testProperties);
		assertSame(testProperties, revengConfigurationWrapper.getProperties());
		// For jpa configuration
		assertNotSame(testProperties, jpaConfigurationWrapper.getProperties());
		wrappedJpaConfiguration.setProperties(testProperties);
		assertSame(testProperties, jpaConfigurationWrapper.getProperties());
	}
	
}