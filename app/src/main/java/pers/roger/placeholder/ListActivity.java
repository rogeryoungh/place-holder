package pers.roger.placeholder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListActivity extends AppCompatActivity {

    RecyclerView listview_main;
    AppsAdapter appsAdapter;
    ProgressBar progressBar;
    TextView loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        listview_main = findViewById(R.id.listview_main);
        progressBar = findViewById(R.id.progressBar);
        loading = findViewById(R.id.loading);

        List<PackageInfo> installedPackages = getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES);
        loading.setText("Loading " + installedPackages.size() + " APPS");

        listview_main.setLayoutManager(new LinearLayoutManager(this));
        listview_main.setItemViewCacheSize(100);
        listview_main.setDrawingCacheEnabled(true);
        listview_main.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        listview_main.post(() -> {
            appsAdapter = new AppsAdapter(this, getPackageManager());
            listview_main.setAdapter(appsAdapter);
            listview_main.setOnClickListener(appsAdapter.clickLisener);
            listview_main.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            loading.setVisibility(View.INVISIBLE);
        });
    }
}


class AppsAdapter extends RecyclerView.Adapter<AppsViewHolder> {
    private static final String TAG = "AppsAdapter";
    AppInfo[] appInfos;
    PackageManager packageManager;
    Context context;
    ClickListener clickLisener;

    public AppsAdapter(Context context, PackageManager packageManager) {
        this.context = context;
        clickLisener = new ClickListener();
        this.packageManager = packageManager;

        List<PackageInfo> catApps = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        appInfos = new AppInfo[catApps.size()];

        int id = 0;
        for (PackageInfo p : catApps) {
            AppInfo info = new AppInfo();
            info.appIcon = packageManager.getApplicationIcon(p.applicationInfo);
            info.appName = packageManager.getApplicationLabel(p.applicationInfo).toString();
            info.appPackageName = p.packageName;
            appInfos[id++] = info;
        }
    }

    @NonNull
    @Override
    public AppsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_app,parent,false);
        itemView.setOnClickListener(clickLisener);
        return new AppsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AppsViewHolder holder, int position) {
        holder.setApps(appInfos[position]);
    }

    @Override
    public int getItemCount() {
        return appInfos.length;
    }

    class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String packageName = v.getTag().toString();
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("simple text", packageName);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copy Success: " + packageName, Toast.LENGTH_SHORT).show();

            Log.i(TAG, packageName);
        }
    }
}

class AppsViewHolder extends RecyclerView.ViewHolder {
    View itemView;
    TextView apps_title;
    TextView apps_pkg;
    ImageView imageView;
    boolean isload = false;

    public AppsViewHolder(@NonNull View itemView) {
        super(itemView);
        this.itemView = itemView;
        apps_title = itemView.findViewById(R.id.apps_title);
        apps_pkg = itemView.findViewById(R.id.apps_pkg);
        imageView = itemView.findViewById(R.id.apps_icon);
    }

    public void setApps(AppInfo info) {
        apps_title.setText(info.appName);
        apps_pkg.setText(info.appPackageName);
        imageView.setImageDrawable(info.appIcon);
        itemView.setTag(info.appPackageName);
        isload = true;
    }
}

class AppInfo {
    Drawable appIcon;
    String appName;
    String appPackageName;
}