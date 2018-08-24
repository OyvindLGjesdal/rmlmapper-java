package be.ugent.rml;

import be.ugent.rml.functions.FunctionLoader;
import be.ugent.rml.records.RecordsFactory;
import be.ugent.rml.store.QuadStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.*;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

abstract class TestCore {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    Executor createExecutor(String mapPath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        // execute mapping file
        URL url = classLoader.getResource(mapPath);
        if (url != null) {
            mapPath = url.getFile();
        }
        File mappingFile = new File(mapPath);
        QuadStore rmlStore = Utils.readTurtle(mappingFile);

        return new Executor(rmlStore,
                new RecordsFactory(new DataFetcher(mappingFile.getParent(), rmlStore)));
    }

    Executor createExecutor(String mapPath, FunctionLoader functionLoader) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        // execute mapping file
        File mappingFile = new File(classLoader.getResource(mapPath).getFile());
        QuadStore rmlStore = Utils.readTurtle(mappingFile);

        return new Executor(rmlStore, new RecordsFactory(new DataFetcher(mappingFile.getParent(), rmlStore)),
                functionLoader);
    }

    Executor doMapping(String mapPath, String outPath) {
        try {
            Executor executor = this.createExecutor(mapPath);
            doMapping(executor, outPath);
            return executor;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            fail();
        }

        return null;
    }

    void doMapping(Executor executor, String outPath) {
        try {
            QuadStore result = executor.execute(null);
            result.removeDuplicates();
            compareStores(result, filePathToStore(outPath), false);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            fail();
        }
    }

    void doMappingExpectError(String mapPath) {
        ClassLoader classLoader = getClass().getClassLoader();

        // execute mapping file
        File mappingFile = new File(classLoader.getResource(mapPath).getFile());
        QuadStore rmlStore = Utils.readTurtle(mappingFile);

        try {
            Executor executor = new Executor(rmlStore, new RecordsFactory(new DataFetcher(mappingFile.getParent(), rmlStore)));
            QuadStore result = executor.execute(null);
        } catch (IOException e) {

        }
    }

    void compareStores(QuadStore store1, QuadStore store2, boolean removeTimestamps) {
        String string1 = store1.toSortedString();
        String string2 = store2.toSortedString();

        if (removeTimestamps) {
            string1 = string1.replaceAll("\"[^\"]*\"\\^\\^<http://www.w3\\.org/2001/XMLSchema#dateTime>", "");
            string2 = string2.replaceAll("\"[^\"]*\"\\^\\^<http://www.w3\\.org/2001/XMLSchema#dateTime>", "");
        }

        assertEquals(string1, string2);
    }

    void compareFiles(String path1, String path2, boolean removeTimestamps) {
        compareStores(filePathToStore(path1), filePathToStore(path2), removeTimestamps);
    }

    QuadStore filePathToStore(String path) {
        ClassLoader classLoader = getClass().getClassLoader();

        // load output file
        File outputFile = new File(classLoader.getResource(path).getFile());
        QuadStore store;

        if (path.endsWith(".nq")) {
            store = Utils.readTurtle(outputFile, RDFFormat.NQUADS);
        } else {
            store = Utils.readTurtle(outputFile);
        }

        return store;
    }
}
