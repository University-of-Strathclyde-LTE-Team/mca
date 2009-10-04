package org.ilrt.mca.harvester.feeds;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.ilrt.mca.Common;
import org.ilrt.mca.dao.AbstractDao;
import org.ilrt.mca.harvester.Harvester;
import org.ilrt.mca.harvester.HttpResolverImpl;
import org.ilrt.mca.harvester.Resolver;
import org.ilrt.mca.harvester.Source;
import org.ilrt.mca.rdf.Repository;
import org.ilrt.mca.vocab.MCA_REGISTRY;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author Mike Jones (mike.a.jones@bristol.ac.uk)
 */
public class FeedHarvesterImpl extends AbstractDao implements Harvester {

    public FeedHarvesterImpl(Repository repository) throws IOException {
        resolver = new HttpResolverImpl();
        this.repository = repository;
        findSources = loadSparql("/sparql/findHarvestableFeeds.rql");
    }


    @Override
    public void harvest() {

        // new date to keep track of the visit
        Date lastVisited = new Date();

        // query registry for list of feeds to harvest
        // get the date that they were last updated
        List<Source> sources = findSources();

        log.info("Found " + sources.size() + " sources to harvest");

        // harvest each source
        for (Source source : sources) {

            log.info("Request to harvest: <" + source.getUrl() + ">");

            // harvest the data
            Model model = resolver.resolve(source, new FeedResponseHandlerImpl());

            model.write(System.out);

            // delete the old data
            repository.deleteAllInGraph(source.getUrl());

            // add the harvested data
            repository.add(source.getUrl(), model);

            // update the last visited date
            RDFNode date = ModelFactory.createDefaultModel()
                    .createTypedLiteral(Common.parseXsdDate(lastVisited), XSDDatatype.XSDdateTime);
            repository.updatePropertyInGraph(Common.AUDIT_GRAPH_URI, source.getUrl(),
                    DC.date, date);

        }

    }


    private List<Source> findSources() {

        List<Source> sources = new ArrayList<Source>();

        Model m = repository.find(findSources);

        if (!m.isEmpty()) {

            ResIterator iterator = m.listSubjectsWithProperty(RDFS.seeAlso);

            while (iterator.hasNext()) {
                sources.add(getDetails(iterator.nextResource()));
            }
        }

        return sources;
    }


    private Source getDetails(Resource resource) {

        Date lastVisited = null;

        String uri = resource.getProperty(RDFS.seeAlso).getResource().getURI();

        if (resource.hasProperty(MCA_REGISTRY.lastVisitedDate)) {
            try {
                lastVisited = Common.parseXsdDate(resource.getProperty(MCA_REGISTRY.lastVisitedDate)
                        .getLiteral().getLexicalForm());
            } catch (ParseException e) {
                log.error(e.getMessage());
            }
        }

        return new Source(uri, lastVisited);
    }


    final private Logger log = Logger.getLogger(FeedHarvesterImpl.class);


    private Resolver resolver;
    private Repository repository;
    private String findSources;
}
