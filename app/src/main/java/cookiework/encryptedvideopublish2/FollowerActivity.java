package cookiework.encryptedvideopublish2;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import cookiework.encryptedvideopublish2.util.HttpUtil;
import cookiework.encryptedvideopublish2.util.JsonUtil;

import static cookiework.encryptedvideopublish2.Constants.SERVER_ADDRESS;
import static cookiework.encryptedvideopublish2.Constants.SHARED_PREFERENCES;
import static java.net.HttpURLConnection.HTTP_OK;

public class FollowerActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;
    private ApproveListTask mAuthTask;
    private ListViewCompat listView;

    private static final String[] options = {"接受关注请求", "拒绝关注请求", "取消"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_list);
        listView = (ListViewCompat) this.findViewById(R.id.main_list);
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.info_requirelist_progress));
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);

        if (mAuthTask == null) {
            showProgress(true);
            mAuthTask = new ApproveListTask(this);
            mAuthTask.execute((Void) null);
        }
    }

    public class ApproveListTask extends AsyncTask<Void, Void, Boolean>{
        private HttpUtil util = new HttpUtil();
        private ArrayList<Follower> infos;
        private Context context;

        public ApproveListTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                if(infos.size() == 0){
                    listView.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_expandable_list_item_1, new String[]{"没有待确认的请求"}));
                } else {
                    listView.setAdapter(new ArrayAdapter<Follower>(context, android.R.layout.simple_expandable_list_item_1, infos));
                }
            } else {
                Toast.makeText(context, R.string.info_network_error, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        @Override
        protected Boolean doInBackground(Void... unused) {
            try{
                SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
                String username = sp.getString("username", null);
                HashMap<String, String> params = new HashMap<>();
                params.put("username", username);
                util.setMethod(HttpUtil.HttpRequestMethod.POST)
                        .setUrl(SERVER_ADDRESS + "/publisher/myfollowers")
                        .setQuery(params)
                        .sendHttpRequest();
                if (util.getResponseCode() != HTTP_OK) {
                    System.out.println(util.getResponseMessage());
                    return false;
                } else {
                    InputStream resultStream = util.getInputStream();
                    String result = HttpUtil.convertInputStreamToString(resultStream);
                    infos = JsonUtil.convertJsonToArray(result, Follower.class);
                    return true;
                }
            }
            catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }

        private class Follower{
            public String name;
            public String tagNum;
            @Override
            public String toString(){
                return name + "(" + tagNum + "个标签)";
            }
        }
    }

    private void showProgress(final boolean show) {
        if (show) {
            progressDialog.show();
        } else {
            progressDialog.hide();
        }
    }

}
