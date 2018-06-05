package me.gurupras.androidcommons;

import org.junit.Test;

import me.gurupras.androidcommons.testing.FakeFile;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void testLargeFileEntryBuildTree() {
        FakeFile root = new FakeFile("/storage/emulated/1", true);
        try {
            Thread.sleep(1000);
        } catch(Exception e) {}

        root.setNumChildren(12000);
        FileEntry entry = new FileEntry(null, root);
        TimeKeeper tk = new TimeKeeper();
        tk.start();
        entry.buildTree();
        tk.stop();
        System.out.println(tk.toString());
    }
}