package org.hibernate.tool.orm.jbt.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.internal.export.common.AbstractExporter;
import org.hibernate.tool.internal.export.common.TemplateHelper;
import org.hibernate.tool.internal.export.java.Cfg2JavaTool;
import org.hibernate.tool.internal.export.java.EntityPOJOClass;
import org.hibernate.tool.internal.export.java.POJOClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HbmExporterExtTest {
	
	private HbmExporterExt hbmExporterExt = null;
	private Configuration cfg = null;
	private File f = null;
	private boolean templateProcessed = false;
	private boolean delegateHasExported = false;


	@TempDir private File tempFolder;
	
	@BeforeEach
	public void beforeEach() {
		cfg = new Configuration();
		f = new File(tempFolder, "foo");
		hbmExporterExt = new HbmExporterExt(cfg, f);
	}
	
	@Test
	public void testConstruction() throws Exception {
		assertTrue(tempFolder.exists());
		assertFalse(f.exists());
		assertNotNull(hbmExporterExt);
		assertSame(f, hbmExporterExt.getProperties().get(ExporterConstants.OUTPUT_FILE_NAME));
		ConfigurationMetadataDescriptor descriptor = (ConfigurationMetadataDescriptor)hbmExporterExt
				.getProperties().get(ExporterConstants.METADATA_DESCRIPTOR);
		assertNotNull(descriptor);
		Field configurationField = ConfigurationMetadataDescriptor.class.getDeclaredField("configuration");
		configurationField.setAccessible(true);
		assertSame(cfg, configurationField.get(descriptor));
	}
	
	@Test
	public void testSetDelegate() throws Exception {
		Object delegate = new Object();
		Field delegateField = HbmExporterExt.class.getDeclaredField("delegateExporter");
		delegateField.setAccessible(true);
		assertNull(delegateField.get(hbmExporterExt));
		hbmExporterExt.setDelegate(delegate);
		assertSame(delegate, delegateField.get(hbmExporterExt));
	}
	
	@Test
	public void testExportPOJO() throws Exception {
		// first without a delegate exporter
		Map<Object, Object> context = new HashMap<>();
		PersistentClass pc = new RootClass(DummyMetadataBuildingContext.INSTANCE);
		pc.setEntityName("foo");
		pc.setClassName("foo");
		POJOClass pojoClass = new EntityPOJOClass(pc, new Cfg2JavaTool());
		TemplateHelper templateHelper = new TemplateHelper() {
		    public void processTemplate(String templateName, Writer output, String rootContext) {
		    	templateProcessed = true;
		    }
		};
		templateHelper.init(null, new String[] {});
		Field templateHelperField = AbstractExporter.class.getDeclaredField("vh");
		templateHelperField.setAccessible(true);
		templateHelperField.set(hbmExporterExt, templateHelper);
		assertFalse(templateProcessed);
		assertFalse(delegateHasExported);
		hbmExporterExt.exportPOJO(context, pojoClass);
		assertTrue(templateProcessed);
		assertFalse(delegateHasExported);
		// now with a delegate exporter
		templateProcessed = false;
		Object delegateExporter = new Object() {
			@SuppressWarnings("unused")
			public void exportPojo(Map<Object, Object> map, Object object, String string) {
				assertSame(map, context);
				assertSame(object, pojoClass);
				assertEquals(string, pojoClass.getQualifiedDeclarationName());
				delegateHasExported = true;
			}
		};
		hbmExporterExt.setDelegate(delegateExporter);
		assertFalse(templateProcessed);
		assertFalse(delegateHasExported);
		hbmExporterExt.exportPOJO(context, pojoClass);
		assertFalse(templateProcessed);
		assertTrue(delegateHasExported);
	}

}
