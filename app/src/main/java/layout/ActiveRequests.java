package layout;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.bhagat.finalyear.ListData;
import com.example.bhagat.finalyear.ProviderHome;
import com.example.bhagat.finalyear.R;
import com.example.bhagat.finalyear.RequestDetails;
import com.example.bhagat.finalyear.RequestsAdapter;
import com.example.bhagat.finalyear.UserDetails;
import com.example.bhagat.finalyear.VolleyNetworkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bhagat on 10/9/16.
 */
public class ActiveRequests extends Fragment implements RequestDetails.RequestDialogResponse,SwipeRefreshLayout.OnRefreshListener {

    ListView requestsList;
    ArrayList<ListData> arrayOfItems;
    RequestsAdapter adapter;
    String request_id = "";
    SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_active_requests, container, false);
        requestsList = (ListView) v.findViewById(R.id.list);
        arrayOfItems = new ArrayList<>();
        //getRequests();
        //the two lines below can be removed
        adapter = new RequestsAdapter(getContext(),0,  arrayOfItems);
        requestsList.setAdapter(adapter);
        //  To open the dialog for details
        requestsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                TextView consumerNameView = (TextView)view.findViewById(R.id.consumer);
                String consumerName = consumerNameView.getText().toString();

                TextView categoryNameView = (TextView)view.findViewById(R.id.category);
                String categoryName = categoryNameView.getText().toString();

                //todo: distance is to be calculated (Eucledean/distance-matrix)

                TextView distanceView = (TextView)view.findViewById(R.id.distance);
                String distance = distanceView.getText().toString();

                TextView quantityView = (TextView)view.findViewById(R.id.quantity);
                String quantity = quantityView.getText().toString();
                try {
                    request_id = arrayOfItems.get(i).jOb.getString("request_id");
                }
                catch (Exception e){
                }
                showDetails(i,consumerName,categoryName,distance,quantity,request_id);
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        swipeRefreshLayout.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(true);
                                        getRequests();
                                    }
                                }
        );
        return v;
    }


    /*private void showDetails(int position)
    {
        RequestDetails dialog = new RequestDetails();
        dialog.show(getFragmentManager(), "dialogTag");
    }*/
    private void showDetails(int position,String consumerName,String categoryName,String distance, String quantity,String request_id) {
        FragmentManager fm = getFragmentManager();
        Bundle args = new Bundle();
        args.putString("consumerName",consumerName);
        args.putString("categoryName",categoryName);
        args.putString("quantity",quantity);
        args.putString("distance",distance);
        args.putString("request_id",request_id);
        args.putInt("listItemPosition", position);
        RequestDetails details = RequestDetails.newInstance();
        details.setTargetFragment(this, 0);
        details.setArguments(args);
        details.show(fm, "activeDialogTag");
    }


    @Override
    public void onDialogResponse(String response, int position) {
        if(response.equals("accept")){
            makeRequestStatusPending();
            arrayOfItems.remove(position);
            requestsList.invalidateViews();
        }
        else if (response.equals("decline")){
            makeRequestStatusCancelled();
            arrayOfItems.remove(position);
            requestsList.invalidateViews();
        }
    }

    public void makeRequestStatusPending(){
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        String url = UserDetails.getInstance().url + "update_request_status.php";
        StringRequest request = new StringRequest( Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("markRequestPending", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                        Log.d("markRequestSeen error",error.toString());
                    }
                }
        )
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("status","2");
                params.put("request_id",request_id);
                return params;
            }
        };
        requestQueue.add(request);
    }


    void makeRequestStatusCancelled(){
        Map<String, String> params = new HashMap<>();
        params.put("status","4");
        params.put("request_id",request_id);
        VolleyNetworkManager.getInstance(getContext()).makeRequest(params, "update_request_status.php",
                new VolleyNetworkManager.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d("makeStatusCancelled",response);
                    }
                });
    }

    @Override
    public void onRefresh() {
        getRequests();
    }


    void getRequests(){
        swipeRefreshLayout.setRefreshing(true);
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        String url = UserDetails.getInstance().url + "fetch_requests.php";
        StringRequest request = new StringRequest( Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("ActiveRequests response", response);
                        try {
                            if(arrayOfItems!=null)
                                arrayOfItems.clear();
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jOb = jsonArray.getJSONObject(i);
                                arrayOfItems.add(new ListData(jOb));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //todo duplicacy in instance of RequestAdapter
                        try{
                         ;
                        }catch (Exception e){
                            Toast.makeText(getActivity(),"Currently you have no new requests.",Toast.LENGTH_LONG).show();
                        }
                        adapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("fetch_requests error",error.toString());
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
        )
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("provider_id", UserDetails.getInstance().providerId);
                params.put("type","active");
                return params;
            }
        } ;
        requestQueue.add(request);
    }


}