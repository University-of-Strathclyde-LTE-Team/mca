/*
 * Copyright (c) 2009, University of Bristol
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the name of the University of Bristol nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ilrt.mca.servlet;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import org.apache.log4j.Logger;
import org.ilrt.mca.rdf.ConnPoolStoreWrapperManagerImpl;
import org.ilrt.mca.rdf.DataManager;
import org.ilrt.mca.rdf.DataSourceManager;
import org.ilrt.mca.rdf.SdbManagerImpl;
import org.ilrt.mca.rdf.StoreWrapper;
import org.ilrt.mca.rdf.StoreWrapperManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * @author Mike Jones (mike.a.jones@bristol.ac.uk)
 */
public class RegistryInitServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {

        log.info("RegistryInitServlet started.");

        super.init(config);

        // find the configuration files
        String configLocation = config.getInitParameter("configLocation");
        String registryLocation = config.getInitParameter("registryLocation");

        // create the dataManager
        DataSourceManager dataSourceManager = new DataSourceManager();
        StoreWrapperManager manager =
                new ConnPoolStoreWrapperManagerImpl(configLocation, dataSourceManager.getDataSource());

        // log the database being used
        StoreWrapper wrapper = manager.getStoreWrapper();
        log.info("Database used: " + wrapper.getStore().getDatabaseType().getName());
        wrapper.close();

        DataManager repository = new SdbManagerImpl(manager);

        // clear existing registry
        log.info("Clearing existing registry details");
        repository.deleteAllInGraph(null);

        // load configuration files
        Model model = ModelFactory.createDefaultModel();

        // load the registry and add it to the database
        log.info("Loading registry details");

        String[] locations = registryLocation.split(",");

        for (String location : locations) {
            log.info("Adding ... " + location);
            model.add(FileManager.get().loadModel(location));
        }

        repository.add(model);

        log.info("Loading OSM amenities data ...");
        Model amenities = ModelFactory.createDefaultModel();
        amenities.read(getClass().getResourceAsStream("/data/osm-amenities.rdf"), null);
        repository.deleteAllInGraph("mca://osm-amenities");
        repository.add("mca://osm-amenities", amenities);
        log.info("added " + amenities.size() + " triples");

        log.info("Loading OSM shop data ...");
        Model shops = ModelFactory.createDefaultModel();
        shops.read(getClass().getResourceAsStream("/data/osm-shops.rdf"), null);
        repository.deleteAllInGraph("mca://osm-shops");
        repository.add("mca://osm-shops", shops);
        log.info("added " + shops.size() + " triples");

        log.info("Registry servlet finished loading data");

    }

    @Override
    public void destroy() {
        log.info("RegistryInitServlet shutdown.");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private final Logger log = Logger.getLogger(RegistryInitServlet.class);
}
