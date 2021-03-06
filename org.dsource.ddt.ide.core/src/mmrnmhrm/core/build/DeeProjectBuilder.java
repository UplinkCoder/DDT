package mmrnmhrm.core.build;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import melnorme.utilbox.misc.MiscUtil;
import mmrnmhrm.core.DeeCore;
import mmrnmhrm.core.projectmodel.DeeProjectModel;
import mmrnmhrm.core.projectmodel.DeeProjectOptions;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IScriptProject;


public class DeeProjectBuilder extends IncrementalProjectBuilder {
	
	public final static String BUILDER_ID = DeeCore.PLUGIN_ID + ".deebuilder";
	
	protected static boolean TESTSMODE = false;
	
	protected static List<IDeeBuilderListener> listeners = Collections.synchronizedList(new LinkedList<IDeeBuilderListener>());
	
	public static void addDataListener(IDeeBuilderListener listener) {
		listeners.add(listener);
	}
	
	public static void removeDataListener(IDeeBuilderListener listener) {
		listeners.remove(listener);
	}
	
	private IFolder outputFolder;
	
	@Override
	protected void startupOnInitialize() {
		assertTrue(getProject() != null);
	}
	
	private IScriptProject getModelProject() throws CoreException {
		IScriptProject scriptProject = DLTKCore.create(getProject());
		if(!scriptProject.exists())
			throw DeeCore.createCoreException("Project: " + scriptProject.getElementName()
					+ " does not exist, or is not a DLTK project.", null);
		return scriptProject;
	}
	
	private DeeProjectOptions getProjectOptions() throws CoreException {
		return DeeProjectModel.getDeeProjectInfo(getModelProject());
	}
	
	
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		IPath outputPath = getProjectOptions().getCompilerOptions().outputDir;
		outputFolder = getProject().getFolder(outputPath);
		if(outputFolder.exists()) {
			for(IResource res : outputFolder.members()) {
				res.delete(true, monitor);
			}
		}
	}
	
	private IFolder prepOutputFolder(DeeProjectOptions options) throws CoreException {
		IPath outputPath = options.getCompilerOptions().outputDir;
		IFolder outputFolder = getProject().getFolder(outputPath);
		
		if(!outputFolder.exists())
			outputFolder.create(IResource.DERIVED, true, null);
		return outputFolder;
	}
	
	@Override
	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
		
		IProject project = getProject();
		DeeBuilder.buildLog.println("Doing build ", kind, " for project:", project);
		
		IScriptProject deeProj = getModelProject();
		DeeBuilder deeBuilder = TESTSMODE ? new NoOpDeeBuilder(deeProj) : new DeeBuilder(deeProj);
		deeBuilder.setListeners(MiscUtil.synchronizedCreateIterable(listeners));
		
		monitor.beginTask("Building D project", 5);
		
		outputFolder = prepOutputFolder(getProjectOptions());
		monitor.worked(1);
		
		deeBuilder.collectBuildUnits(monitor);
		monitor.worked(1);
		
		deeBuilder.compileModules(monitor);
		monitor.worked(1);
		
		deeBuilder.runBuilder(monitor);
		monitor.worked(1);
		
		
		outputFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		return null; // No deps
	}
	
	public static class NoOpDeeBuilder extends DeeBuilder {
		
		public NoOpDeeBuilder(IScriptProject deeProj) throws CoreException {
			super(deeProj);
		}
		
		@Override
		protected void startProcess(IProgressMonitor monitor, ProcessBuilder builder) throws CoreException {
		}
		
	}
	
}
