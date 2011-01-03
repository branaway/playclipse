package org.playframework.playclipse.builder;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.playframework.playclipse.FilesAccess;
import org.playframework.playclipse.PlayPlugin;
import org.playframework.playclipse.editors.html.HTMLEditor;
import org.playframework.playclipse.editors.route.RouteEditor;

public class PlayBuilder extends IncrementalProjectBuilder implements IPropertyChangeListener {

	public PlayBuilder() {
		super();
		PlayPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	class ResourceVisitor implements IResourceVisitor {
		@Override
		public boolean visit(IResource resource) {
			if (!(resource instanceof IFile))
				return true;
			IFile file = (IFile) resource;
			if (resource.getName().equals("routes")) {
				deleteMarkers(file);
				(new RouteChecker(file, RouteEditor.MISSING_ROUTE)).check();
				return false;
			}
			if (TemplateChecker.isTemplate(resource.getFullPath())) {
				deleteMarkers(file);
				(new TemplateChecker(file, HTMLEditor.MISSING_ACTION)).check();
				return false;
			}
			return true;
		}
	}

	public static final String BUILDER_ID = "org.playframework.playclipse.PlayBuilder";

	@Override
	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
		IResourceDelta delta = getDelta(getProject());
		switch (kind) {
		case FULL_BUILD:
			System.out.println("-- full build");
			fullBuild(monitor);
			break;
		case INCREMENTAL_BUILD:
			System.out.println("-- inc build");
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
			break;
		case AUTO_BUILD: // auto-incremental
			System.out.println("-- auto build");
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
			break;
		default:
			break;

		}
		return null;
	}
	
	

	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) {
		try {
			ensureJapidViewsDir();
			delta.accept(new JapidDeltaVisitor());
		} catch (CoreException e) {
			PlayPlugin.showError(e);
		}
	}

	private void ensureJapidViewsDir() {
		IProject proj = getProject();
		FilesAccess.prepareFolder(proj.getFolder("app/japidviews/_layouts"));
		FilesAccess.prepareFolder(proj.getFolder("app/japidviews/_tags"));
		FilesAccess.prepareFolder(proj.getFolder("app/japidviews/_javatags"));
		FilesAccess.prepareFolder(proj.getFolder("app/japidviews/_notifiers"));
		IFile file = proj.getFile("app/japidviews/_javatags/JapidWebUtil.java");
		if (!file.exists()) {
			try {
				file.create(new ByteArrayInputStream(JAPID_WEB_UTIL.getBytes("UTF-8")), true, null);
			} catch (Exception e) {
				PlayPlugin.showError(e);
			}
		}

		file = proj.getFile("app/japidviews/_layouts/SampleLayout.html");
		if (!file.exists()) {
			try {
				file.create(new ByteArrayInputStream(SAMPLE_LAYOUT.getBytes("UTF-8")), true, null);
			} catch (Exception e) {
				PlayPlugin.showError(e);
			}
		}

		file = proj.getFile("app/japidviews/_tags/SampleTag.html");
		if (!file.exists()) {
			try {
				file.create(new ByteArrayInputStream(SAMPLE_TAG.getBytes("UTF-8")), true, null);
			} catch (Exception e) {
				PlayPlugin.showError(e);
			}
		}
	}
	
	private static final String SAMPLE_TAG = "`	args String a\n" + 
			"Hi $a!\n" + 
			"";
	private static final String SAMPLE_LAYOUT = "A sample layout.\n" + 
			"<p>\n" + 
			"#{get 'title'/};\n" + 
			"</p>\n" + 
			"<div>\n" + 
			"#{doLayout /}\n" + 
			"</div>\n" + 
			"";

	private static final String JAPID_WEB_UTIL = "package japidviews._javatags;\n" + 
			"\n" + 
			"/**\n" + 
			" * a well-know place to add all the static method you want to use in your\n" + 
			" * templates.\n" + 
			" * \n" + 
			" * All the public static methods will be automatically \"import static \" to the\n" + 
			" * generated Java classes by the Japid compiler.\n" + 
			" * \n" + 
			" */\n" + 
			"public class JapidWebUtil {\n" + 
			"	public static String hi() {\n" + 
			"		return \"Hi\";\n" + 
			"	}\n" + 
			"	// your utility methods...\n" + 
			"	\n" + 
			"}\n" + 
			"";

	void checkRoute(IFile file) {
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		ensureJapidViewsDir();
		try {
			getProject().accept(new ResourceVisitor());
			getProject().accept(new JapidFullBuildVisitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static Set<String> observedProperties = new HashSet<String>();
	static {
		observedProperties.add(RouteEditor.MISSING_ROUTE);
		observedProperties.add(HTMLEditor.MISSING_ACTION);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		for (String property : observedProperties) {
			if (event.getProperty().equals(property)) {
				try {
					fullBuild(null);
				} catch (CoreException e) {
				}
				return;
			}
		}
	}



	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		// chance to clean all generated Japid code
		super.clean(monitor);
	}

	
}
