package me.gurupras.androidcommons;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class FilesAdapter extends BaseAdapter {
    private static final String TAG = FileExplorerFragment.TAG + "->FilesAdapter";
    private LayoutInflater inflater;
    private List<FilesAdapter> childAdapterList;
    private FileEntry entry;
    private FilesAdapter parentAdapter;

    private FilesAdapter(FileEntry entry, FilesAdapter parent) {
        inflater = (LayoutInflater) AndroidApplication.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.parentAdapter = parent;
        this.entry = entry;
        this.childAdapterList = new LinkedList<FilesAdapter>();
    }


    public List<FilesAdapter> getChildAdapterList() {
        return childAdapterList;
    }

    public FileEntry getEntry() {
        return entry;
    }

    public FilesAdapter getParentAdapter() {
        return parentAdapter;
    }

    public static FilesAdapter buildListAdapter(FileEntry root) {
        return buildListAdapter(root, null);
    }

    private static FilesAdapter buildListAdapter(FileEntry entry, FilesAdapter parent) {
        FilesAdapter adapter = new FilesAdapter(entry, parent);
        adapter.build();
        return adapter;
    }

    public void build(final SortType sortBy) {
        TimeKeeper tk = new TimeKeeper();

        this.getEntry().buildTree();
        int numChildren = this.getCount();
        if(numChildren == 0 || numChildren != this.getEntry().size()) {
            tk.start();

            this.childAdapterList.clear();

            // Always add . and ..
            FileEntry dot = new FileEntry(this.getEntry(), ".");
            FileEntry dotdot = new FileEntry(this.getEntry(), "..");
            this.childAdapterList.add(new FilesAdapter(dot, this));
            this.childAdapterList.add(new FilesAdapter(dotdot, this));

            List<FileEntry> sortedEntries = new LinkedList<FileEntry>(entry);
            Collections.sort(sortedEntries, new Comparator<FileEntry>() {
                @Override
                public int compare(FileEntry o1, FileEntry o2) {
                    switch (sortBy) {
                        case DEFAULT:
                            return -1;
                        case NAME:
                            return o2.getName().compareTo(o1.getName());
                        case SIZE:
                            return Double.compare(o2.getFile().length(), o1.getFile().length());
                        case LAST_MODIFIED:
                            return Double.compare(o2.getFile().lastModified(), o1.getFile().lastModified());
                        default:
                            throw new IllegalStateException("Unimplemented sortBy type: " + sortBy.name());
                    }
                };
            });
            for(FileEntry child : sortedEntries) {
                FilesAdapter childAdapter = new FilesAdapter(child, this);
                this.childAdapterList.add(childAdapter);
            }
        }
        else
            tk.start();
        if(this.getCount() == 0) {
        }

        tk.stop();
        Log.d(TAG + "->build", "Time Taken :" + tk);
    }

    public void build() {
        build(SortType.DEFAULT);
    }

    @Override
    public Object getItem(int position) {
        return childAdapterList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return childAdapterList.get(position).hashCode();
    }

    @Override
    public int getCount() {
        return childAdapterList.size();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final TextView text;

        if (convertView == null) {
            view = inflater.inflate(R.layout.listitem, null);
        } else {
            view = convertView;
        }

        FileEntry entry = this.childAdapterList.get(position).getEntry();
        TextView filename = (TextView) view.findViewById(R.id.listitem_filename);
        TextView lastModified = (TextView) view.findViewById(R.id.listitem_last_modified);
        TextView size = (TextView) view.findViewById(R.id.listitem_size);
        filename.setText(entry.getName());
        lastModified.setText(new Date(entry.getFileMetadata().getFile().lastModified()).toString());
        if(entry.isDir()) {
            size.setText("");
        } else {
            size.setText(Helper.humanReadableByteCount(entry.getFile().length(), true));
        }
        return view;
    }

    public enum SortType {
        DEFAULT,
        NAME,
        SIZE,
        LAST_MODIFIED,
    }
}
