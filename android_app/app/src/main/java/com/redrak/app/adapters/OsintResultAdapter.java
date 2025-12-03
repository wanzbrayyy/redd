package com.redrak.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.redrak.app.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OsintResultAdapter extends RecyclerView.Adapter<OsintResultAdapter.ViewHolder> {

    private final List<JSONObject> results = new ArrayList<>();
    private final List<String> resultKeys = new ArrayList<>();
    private final Context context;

    public OsintResultAdapter(Context context) {
        this.context = context;
    }

    public void updateData(JSONObject listObject) {
        results.clear();
        resultKeys.clear();
        if (listObject != null) {
            Iterator<String> keys = listObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject dbObject = listObject.optJSONObject(key);
                if (dbObject != null) {
                    results.add(dbObject);
                    resultKeys.add(key);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject item = results.get(position);
        String key = resultKeys.get(position);
        holder.bind(item, key);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDatabaseName;
        final LinearLayout layoutDataContainer;

        ViewHolder(View itemView) {
            super(itemView);
            tvDatabaseName = itemView.findViewById(R.id.tvDatabaseName);
            layoutDataContainer = itemView.findViewById(R.id.layoutDataContainer);
        }

        void bind(JSONObject item, String dbName) {
            tvDatabaseName.setText(dbName);
            layoutDataContainer.removeAllViews();

            JSONArray dataArray = item.optJSONArray("Data");
            if (dataArray != null && dataArray.length() > 0) {
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject dataObject = dataArray.optJSONObject(i);
                    if (dataObject != null) {
                        Iterator<String> dataKeys = dataObject.keys();
                        while (dataKeys.hasNext()) {
                            String dataKey = dataKeys.next();
                            String dataValue = dataObject.optString(dataKey);
                            TextView dataTextView = new TextView(context);
                            dataTextView.setText(dataKey + ": " + dataValue);
                            dataTextView.setTextIsSelectable(true);
                            layoutDataContainer.addView(dataTextView);
                        }
                    }
                }
            } else {
                TextView noData = new TextView(context);
                noData.setText("No detailed data available.");
                layoutDataContainer.addView(noData);
            }
        }
    }
}
