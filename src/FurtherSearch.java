
// Import the necessary libraries
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.api.ParsedResponse;
import com.nextcloud.android.sso.exceptions.NextcloudHttpRequestFailedException;
import com.nextcloud.android.sso.model.SingleSignOnAccount;
import com.nextcloud.android.sso.ui.UiExceptionManager;
import com.nextcloud.android.sso.utils.FileStorageUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Declare the variables
    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private List<File> fileList;
    private NextcloudAPI nextcloudAPI;
    private String query;

    // Override the onCreate method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the variables
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileList);
        recyclerView.setAdapter(fileAdapter);

        // Get the intent and handle the search query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            performSearch(query);
        }
    }

    // Override the onCreateOptionsMenu method
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Get the search manager and the search view
        // a guide on how to create a search interface for your android app 2. It shows
        // how to use the search dialog or search widget, and how to add recent query
        // suggestions and custom suggestions.
        // https://developer.android.com/develop/ui/views/search
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        // Set the searchable info and the query hint
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint(getString(R.string.search_hint));

        // Return true to display the menu
        return true;
    }

    // A method to perform the search query
    private void performSearch(String query) {
        // Clear the previous results
        fileList.clear();
        fileAdapter.notifyDataSetChanged();

        // Get the current account and create a new nextcloud API instance
        SingleSignOnAccount account = FileStorageUtils.getCurrentSingleSignOnAccount(this);
        nextcloudAPI = new NextcloudAPI(this, account, new GsonBuilder().create(),
                new NextcloudAPI.ApiConnectedListener() {
                    @Override
                    public void onConnected() {
                        // Nothing to do here
                    }

                    @Override
                    public void onError(Exception ex) {
                        // Handle the error
                        UiExceptionManager.showDialogForException(MainActivity.this, ex);
                    }
                });

        // Create a new thread to perform the network request
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Create a JSON object with the search parameters
                    JSONObject params = new JSONObject();
                    params.put("query", query);
                    params.put("page", 1);
                    params.put("size", 10);
                    params.put("providers", new JSONArray().put("files"));

                    // Perform a POST request to the fulltextsearch service
                    // https://github.com/nextcloud/fulltextsearch
                    // GitHub repository that contains the core app of a full-text search framework
                    // for nextcloud 1. It explains how to set up your application to use
                    // Elasticsearch, a Java-based search engine, and how to install some provider
                    // apps that extract content from your nextcloud.
                    ParsedResponse<String> response = nextcloudAPI.performNetworkRequest(
                            "POST",
                            "/index.php/apps/fulltextsearch/v1/search",
                            params.toString());

                    // Parse the response as a JSON object
                    JSONObject result = new JSONObject(response.getResponse());

                    // Check if the status is ok
                    if (result.getString("status").equals("ok")) {
                        // Get the search results as a JSON array
                        JSONArray documents = result.getJSONArray("documents");

                        // Loop through the documents and create a list of files
                        for (int i = 0; i < documents.length(); i++) {
                            // Get the document as a JSON object
                            JSONObject document = documents.getJSONObject(i);

                            // Get the file information as a JSON object
                            JSONObject info = document.getJSONObject("info");

                            // Create a new file object and add it to the list
                            File file = new File();
                            file.setId(info.getInt("fileid"));
                            file.setName(info.getString("name"));
                            file.setPath(info.getString("path"));
                            file.setSize(info.getLong("size"));
                            file.setModified(info.getLong("mtime"));
                            file.setMimetype(info.getString("mimetype"));
                            fileList.add(file);
                        }

                        // Update the UI on the main thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Notify the adapter of the data change
                                fileAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (NextcloudHttpRequestFailedException e) {
                    // Handle the HTTP exception
                    e.printStackTrace();
                } catch (IOException e) {
                    // Handle the IO exception
                    e.printStackTrace();
                } catch (Exception e) {
                    // Handle any other exception
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
