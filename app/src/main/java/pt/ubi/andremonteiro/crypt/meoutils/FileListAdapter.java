package pt.ubi.andremonteiro.crypt.meoutils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import pt.ubi.andremonteiro.crypt.R;

/**
 * Created by André Monteiro on 14/06/2016.
 */
public class FileListAdapter extends ArrayAdapter<YubicryptFile>{

    public FileListAdapter(Context context, int resource, ArrayList<YubicryptFile> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        YubicryptFile file = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.file_list_layout, parent, false);
        }
        // Lookup view for data population
        TextView fileName = (TextView) convertView.findViewById(R.id.fileFilenameText);
        TextView fileSize = (TextView) convertView.findViewById(R.id.fileSizeText);
        TextView fileModified = (TextView) convertView.findViewById(R.id.fileModifiedText);
        // Populate the data into the template view using the data object
        fileName.setText(file.getFileName());
        fileSize.setText(file.getSize());
        fileModified.setText(file.getModified());
        // Return the completed view to render on screen
        return convertView;
    }

}
