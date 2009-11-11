package org.ilrt.mca.dao.delegate;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.ilrt.mca.dao.AbstractDao;
import org.ilrt.mca.domain.Item;
import org.ilrt.mca.domain.contacts.ContactImpl;
import org.ilrt.mca.domain.directory.DirectoryImpl;
import org.ilrt.mca.rdf.Repository;
import org.ilrt.mca.vocab.FOAF;
import org.ilrt.mca.vocab.MCA_REGISTRY;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

/**
 * @author Mike Jones (mike.a.jones@bristol.ac.uk)
 */
public class DirectoryDelegateImpl extends AbstractDao implements Delegate {

    public DirectoryDelegateImpl(final Repository repository) {
        this.repository = repository;
        try {
        	findDirectorySparql = loadSparql("/sparql/findDirectoryDetails.rql");
        } catch (IOException ex) {
            log.error("Unable to load SPARQL query: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Item createItem(Resource resource, MultivaluedMap<String, String> parameters) {

        DirectoryImpl directoryImpl = new DirectoryImpl();

        if (resource.hasProperty(MCA_REGISTRY.detailsUrlStem)) {
        	directoryImpl.setDetailsUrlStem(resource.getProperty(MCA_REGISTRY.detailsUrlStem).getString());
        }

        if (resource.hasProperty(MCA_REGISTRY.queryUrlStem)) {
        	directoryImpl.setQueryUrlStem(resource.getProperty(MCA_REGISTRY.queryUrlStem).getString());
        }

        getBasicDetails(resource, directoryImpl);

        return directoryImpl;
    }

    @Override
    public Model createModel(Resource resource, MultivaluedMap<String, String> parameters) {

        Model model = repository.find("id", resource.getURI(), findDirectorySparql);

        return ModelFactory.createUnion(resource.getModel(), model);
    }

    private String findDirectorySparql = null;
    private final Repository repository;
    Logger log = Logger.getLogger(DirectoryDelegateImpl.class);
}
