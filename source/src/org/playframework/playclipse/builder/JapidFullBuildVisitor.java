package org.playframework.playclipse.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

import bran.japidplugin.TemplateTransformer;
import cn.bran.japid.classmeta.AbstractTemplateClassMetaData;
import cn.bran.japid.util.DirUtil;
import cn.bran.play.JapidPlayAdapter;
import cn.bran.play.JapidPlugin;
import cn.bran.play.NoEnhance;
import cn.bran.play.WebUtils;

public class JapidFullBuildVisitor implements IResourceVisitor {
	static {
		initTemplateCLassMeta();
	}

	public static void initTemplateCLassMeta() {
		AbstractTemplateClassMetaData.addImportStatic(JapidPlayAdapter.class);
		AbstractTemplateClassMetaData.addImportStaticGlobal("play.data.validation.Validation");
		AbstractTemplateClassMetaData.addImportStaticGlobal("play.templates.JavaExtensions");
		AbstractTemplateClassMetaData.addImportStatic(WebUtils.class);
		AbstractTemplateClassMetaData.addAnnotation(NoEnhance.class);
		AbstractTemplateClassMetaData.addImportLineGlobal(JapidPlugin.JAPIDVIEWS_ROOT + "._layouts.*");
		AbstractTemplateClassMetaData.addImportLineGlobal(JapidPlugin.JAPIDVIEWS_ROOT + "._javatags.*");
		AbstractTemplateClassMetaData.addImportLineGlobal(JapidPlugin.JAPIDVIEWS_ROOT + "._tags.*");
		AbstractTemplateClassMetaData.addImportLineGlobal("play.mvc.Scope.*");
		AbstractTemplateClassMetaData.addImportLineGlobal("play.mvc.Http.*");
		AbstractTemplateClassMetaData.addImportLineGlobal("play.data.validation.Validation");
		AbstractTemplateClassMetaData.addImportLineGlobal("play.data.validation.Error");
		AbstractTemplateClassMetaData.addImportLineGlobal("models.*");
		AbstractTemplateClassMetaData.addImportLineGlobal("controllers.*");
		AbstractTemplateClassMetaData.addImportLineGlobal("static  japidviews._javatags.JapidWebUtil.*");
	}

	@Override
	public boolean visit(IResource res) throws CoreException {
		if (res instanceof IFile) {
			convertTemplate(res);
		}
		return true;
	}
	/**
	 * @param res
	 * @throws CoreException
	 */
	public static void convertTemplate(IResource res) throws CoreException {
		IFile f = ((IFile)res);
		if (JapidDeltaVisitor.isTemplateSource(f)) {
			InputStream is = transform(f);

			String templateJavaFile  = DirUtil.mapSrcToJava(f.getProjectRelativePath().toString());
			IFile jFile = f.getProject().getFile(templateJavaFile);
			
				if (!jFile.exists()) {
					jFile.create(is, true, null);
					jFile.setDerived(true, null);
				} else {
//					jFile.setContents(is, IFile.FORCE, null);
					jFile.setContents(is, 0, null); // don't use force seems safer in case of out of sync
					jFile.setDerived(true, null);
				}
		}
	}

	private static InputStream transform(IFile f) {
		try {
			String code = TemplateTransformer.generate(f);
			return new ByteArrayInputStream(code.getBytes("UTF-8"));
		} catch (Exception e) {
//			ByteArrayOutputStream out = new ByteArrayOutputStream();
//			PrintStream ps = new PrintStream(out, true);
//			e.printStackTrace(ps);
			// put the compiling error in the generated file to get the attention.
			String err = "~~Japid compiler generated message:\n~\n" + "Error in compiling file: " + f.getName() + ". The error message is:\n~\n" + e.getMessage();
			err += "\n~\nPlease fix the error the template file and this file will be re-generated. ";
			try {
				return new ByteArrayInputStream(err.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				throw new RuntimeException(err);
			}
		}
	}

}