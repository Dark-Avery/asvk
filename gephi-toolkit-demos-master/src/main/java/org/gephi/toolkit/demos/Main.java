/*
Copyright 2008-2010 Gephi
Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gephi.toolkit.demos;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import javax.swing.JFrame;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.preview.PNGExporter;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.preview.api.*;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.toolkit.demos.plugins.preview.PreviewSketch;
import org.openide.util.Lookup;

import static java.lang.Boolean.TRUE;

/**
 *
 * @author Mathieu Bastian
 */
public class Main
{

    public void script() throws IOException {
        //Init a project - and therefore a workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();

        //Import file
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        Container container;
        try {
            File file = new File(getClass().getResource("/org/gephi/toolkit/demos/infiniband_copy.gexf").toURI());
            container = importController.importFile(file);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        //Append imported data to GraphAPI
        importController.process(container, new DefaultProcessor(), workspace);

        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();

        ForceAtlas2 layout = new ForceAtlas2(null);
        layout.setGraphModel(graphModel);
        layout.initAlgo();
        layout.setAdjustSizes(true);
        layout.setScalingRatio(10.0);
        layout.setBarnesHutOptimize(false);

        for (int i = 0; i < 10000 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }
        layout.endAlgo();

        //Preview configuration
        PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
        PreviewModel previewModel = previewController.getModel();
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, TRUE);
        previewModel.getProperties().putValue(PreviewProperty.SHOW_EDGES, TRUE);
        Font nodeLabelFont = new Font("Arial", Font.PLAIN, 2);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, nodeLabelFont);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 50);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GREEN));

        //New Processing target, get the PApplet
        G2DTarget target = (G2DTarget) previewController.getRenderTarget(RenderTarget.G2D_TARGET);
        final PreviewSketch previewSketch = new PreviewSketch(target);
        previewController.refreshPreview();

        //Add the applet to a JFrame and display
        JFrame frame = new JFrame("Graph Preview");
        frame.setLayout(new BorderLayout());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(previewSketch, BorderLayout.CENTER);

        frame.setSize(1024, 768);
        
        //Wait for the frame to be visible before painting, or the result drawing will be strange
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                previewSketch.resetZoom();
            }
        });
        frame.setVisible(true);

        File fileToSave = new File("test.png");
        OutputStream fos = new FileOutputStream(fileToSave);

        PNGExporter pnex = new PNGExporter();
        pnex.setWorkspace(workspace);
        pnex.setOutputStream(fos);
        pnex.execute();

        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        ec.exportFile(new File("test.pdf"));
    }

    public static void main(String[] args) throws IOException {
        Main previewJFrame = new Main();
        previewJFrame.script();
    }

}
