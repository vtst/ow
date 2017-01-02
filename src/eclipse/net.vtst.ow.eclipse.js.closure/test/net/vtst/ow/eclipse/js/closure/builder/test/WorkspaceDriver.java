package net.vtst.ow.eclipse.js.closure.builder.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;


public class WorkspaceDriver {

    private IWorkspace workspace;
    private Map<String, IProject> projects;
    private boolean isOpen;

    public void openEmptyWorkspace() {
        workspace = ResourcesPlugin.getWorkspace();
        projects = new HashMap<String, IProject>();
        isOpen = true;
    }

    public void resetWorkspace() {
        if (projects != null) {
            for (IProject project : projects.values()) {
                try {
                    project.delete(true, null);
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void addProject(String name) {
        assert isOpen : "workspace must be open";
        IWorkspaceRoot root = workspace.getRoot();

        final IProject project = root.getProject(name);

        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

            @Override
            public void run(IProgressMonitor monitor) throws CoreException {
                project.create(null, null);
                project.open(null);
                IProjectDescription description = project.getDescription();
                description.setNatureIds(new String[] {ClosureNature.NATURE_ID, JavaScriptCore.NATURE_ID});
                project.setDescription(description, null);
                IJavaScriptProject javaScriptProject = JavaScriptCore.create(project);
                //javaScriptProject.set //set use closure compiler
            }
        };
        try {
            workspace.run(runnable, null);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        projects.put(name, project);
    }


    public void close() {
        // TODO Auto-generated method stub
        isOpen = false;

    }

    public void fullBuild(String projectName) {
        try {
            projects.get(projectName).build(IncrementalProjectBuilder.FULL_BUILD, null);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    public void incrementalBuild(String projectName) {
        try {
            projects.get(projectName).build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    public void shareProblems(ClosureProjectMarkerRetriever retriever) {
        retriever.loadFromProjects(projects);
    }

    
    private IFolder createFolder(IPath path) {
        assert !path.isRoot() : "root";

        /* don't create folders for projects */
        if (path.segmentCount() <= 1) {
            return null;
        }

        IFolder folder = workspace.getRoot().getFolder(path);
        if (!folder.exists()) {
            /* create the parent folder if necessary */
            createFolder(path.removeLastSegments(1));

            try {
                folder.create(true, true, null);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
        return folder;
    }
    
    public void addSourceFolder(String path, String projectName) {
        IProject project = projects.get(projectName);
        IPath folderPath = project.getFullPath().append(path);

        createFolder(folderPath);
        
        try {
            
            //add to javascript folder include path
            IIncludePathEntry pathEntry = JavaScriptCore.newSourceEntry(folderPath.makeRelativeTo(project.getFullPath()).makeAbsolute());
            IJavaScriptProject javaScriptProject = JavaScriptCore.create(project);
            IIncludePathEntry[] oldIncludePath = javaScriptProject.getRawIncludepath();
            
            if (! Arrays.asList(oldIncludePath).contains(pathEntry)) {
            
                IIncludePathEntry[] newIncludePath = new IIncludePathEntry[oldIncludePath.length + 1];
                System.arraycopy(oldIncludePath, 0, newIncludePath, 0, oldIncludePath.length);
                newIncludePath[oldIncludePath.length] = pathEntry;
                javaScriptProject.setRawIncludepath(newIncludePath, null);
            }
            
        } catch (CoreException e) {

            throw new RuntimeException(e);
        }
        
    }
}
