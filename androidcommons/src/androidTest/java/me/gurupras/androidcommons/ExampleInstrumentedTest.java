package me.gurupras.androidcommons;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

import me.gurupras.androidcommons.testing.FakeFile;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("me.gurupras.androidcommons.test", appContext.getPackageName());
    }

    public void generateRandomFile() {

    }

    @Test
    public void testLargeFilesAdapter() {
        FakeFile root = new FakeFile("/storage/emulated/1", true);
        try {
            Thread.sleep(5000);
        } catch(Exception e) {}

        root.setNumChildren(3500);
        FileEntry rootEntry = new FileEntry(null, root);
        TimeKeeper tk = new TimeKeeper();
        tk.start();
        FilesAdapter f = FilesAdapter.buildListAdapter(rootEntry);
        tk.stop();
        System.out.println(tk.toString());
    }
}
