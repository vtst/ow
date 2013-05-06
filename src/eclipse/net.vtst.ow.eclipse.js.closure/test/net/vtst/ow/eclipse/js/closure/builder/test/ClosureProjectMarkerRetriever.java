package net.vtst.ow.eclipse.js.closure.builder.test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;


public class ClosureProjectMarkerRetriever {

    private Map<String, IProject> projects;

    public ClosureProjectMarkerRetriever(WorkspaceDriver driver) {
        driver.shareProblems(this);
    }

    public void loadFromProjects(Map<String, IProject> projects) {
        this.projects = projects;
    }

    public Set<IMarker> getMarkers(String... markerTypes) {
        try {

            Set<IMarker> markers = new HashSet<IMarker>();

            for (IProject project : projects.values()) {
                for (String markerType : markerTypes) {
                    for (IMarker marker : project.findMarkers(markerType, true,
                            IResource.DEPTH_INFINITE)) {
                        markers.add(marker);
                    }
                }
            }

            return markers;

        } catch (CoreException e) {
            throw new RuntimeException(e);
        }

    }



}
