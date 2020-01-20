/*
 * MIT License
 *
 * Copyright (c) 2020 Manuel Gundlach <manuel.gundlach@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dowscr.manuel.prass;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import static java.lang.Math.max;

//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Statistics#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Statistics extends Fragment implements PrinterConstants {

    //    private OnFragmentInteractionListener mListener;
    private View rootView;
    private LayoutInflater stinflater;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public Statistics() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static Statistics newInstance() {
        Statistics fragment = new Statistics();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private int badnessColor(int jobs, int load) {
        double good = max((double) 0, Math.exp(-(double) jobs / 6 - (double) load / 1024 / 1024 / 20));
        return Color.HSVToColor(new float[]{(float) (120 * good), 1, 1});
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootView = inflater.inflate(R.layout.fragment_statistics, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh);

        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        updateStatistics();
                    }
                }
        );


        final TextView[] Pr = {(TextView) rootView.findViewById(R.id.DescPrA), (TextView) rootView.findViewById(R.id.DescPrB), (TextView) rootView.findViewById(R.id.DescPrC)};
        for (int i = 0; i < Pr.length; i++)
            Pr[i].setText(PrinterNames[i]);

        final TextView[] Jo = {(TextView) rootView.findViewById(R.id.JoA), (TextView) rootView.findViewById(R.id.JoB), (TextView) rootView.findViewById(R.id.JoC)};
        for (TextView i : Jo)
            i.setText("?");

        final TextView[] Si = {(TextView) rootView.findViewById(R.id.SiA), (TextView) rootView.findViewById(R.id.SiB), (TextView) rootView.findViewById(R.id.SiC)};
        for (TextView i : Si)
            i.setText("?");

        final TextView[] Re = {(TextView) rootView.findViewById(R.id.ReA), (TextView) rootView.findViewById(R.id.ReB), (TextView) rootView.findViewById(R.id.ReC)};
        for (TextView i : Re)
            i.setText("?");

        updateStatistics();


        stinflater = inflater;
        // Inflate the layout for this fragment
        return rootView;
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_statistics, menu);

//        Menu stmenu = menu;
    }

    private void updateStatistics() {

        final TextView TV = (TextView) rootView.findViewById(R.id.tv);

        final TextView[] Jo = {(TextView) rootView.findViewById(R.id.JoA), (TextView) rootView.findViewById(R.id.JoB), (TextView) rootView.findViewById(R.id.JoC)};

        final TextView[] Si = {(TextView) rootView.findViewById(R.id.SiA), (TextView) rootView.findViewById(R.id.SiB), (TextView) rootView.findViewById(R.id.SiC)};

        final TextView[] Re = {(TextView) rootView.findViewById(R.id.ReA), (TextView) rootView.findViewById(R.id.ReB), (TextView) rootView.findViewById(R.id.ReC)};

        final TableRow[] Row = {(TableRow) rootView.findViewById(R.id.RowA), (TableRow) rootView.findViewById(R.id.RowB), (TableRow) rootView.findViewById(R.id.RowC)};

        final TextView EB = (TextView) rootView.findViewById(R.id.errbox);

        try {
            for (TableRow i : Row)
                i.setBackgroundColor(Color.TRANSPARENT);
            TV.setText("...");
            for (TextView i : Jo)
                i.setText(".");
            for (TextView i : Si)
                i.setText(".");
            for (TextView i : Re)
                i.setText(".");
            EB.setVisibility(View.GONE);
            (new DownloadWorkloads()).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {

            mSwipeRefreshLayout.setRefreshing(true);
            updateStatistics();

        }

        return super.onOptionsItemSelected(item);
    }

    private void resetUpdating() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private class DownloadWorkloads extends AsyncTask<Void, Void, int[][]> {
        String Ex = null;

        protected int[][] doInBackground(Void... s) {
            try {
                int[][] S = new int[NumPrinters + 1][3];
                String command = "printquota";
                //To speed up usage of ssh, multiple commandos are concatenated
                //and sent at once. Result will be separated by string "separator"
                final String separator = "64251674245076664132";
                for (int i = 0; i < NumPrinters; i++)
                    command += "&&echo -n \"" + separator + "\"&&" + "lpq -P " + PrinterIDS[i];
                String Result = ConnectionHandler.executeRemoteCommand(command);
                String[] listofqueues = Result.split(separator);

//                Extract printquota from result
                int printquota = Integer.parseInt((listofqueues[0].split("\\s+"))[6]);
                S[NumPrinters][0] = printquota;


//                Extract stats on single printers from result
                for (int pr = 0; pr < NumPrinters; pr++) {
                    String[] t = listofqueues[pr + 1].split("\\n+");
                    String[] PrinterInfo = t[0].split("\\s+");
                    String HeadLine = t[1];
                    int[] R = new int[3];
                    R[2] = (PrinterInfo.length >= 5 && PrinterInfo[4].equals("printing")) ? 2 : ((PrinterInfo.length >= 3 && PrinterInfo[2].equals("ready")) ? 1 : 0);
                    if (HeadLine.equals("no entries"))
                        //noinspection ConstantConditions
                        R[0] = R[1] = 0;
                    else {
                        int off = 2;
                        //noinspection ConstantConditions
                        R[0] = R[1] = 0;
                        for (int i = off; i < t.length; i++) {
                            R[0]++;
                            String[] tt = t[i].split("\\s+");
                            R[1] += Integer.valueOf(tt[tt.length - 2]);
                        }
                    }
                    S[pr] = R;
                }

                return S;
            } catch (Exception e) {
                Ex = e.getMessage();
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(int[][] s) {

            if (Ex != null) {
                final TextView EB = (TextView) rootView.findViewById(R.id.errbox);
                EB.setText(Ex);
                EB.setVisibility(View.VISIBLE);
            }

            final TextView TV = (TextView) rootView.findViewById(R.id.tv);
            if (s == null || s[NumPrinters] == null)
                TV.setText("ERR");
            else {
                int printquota = s[NumPrinters][0];
                TV.setText(String.valueOf(printquota));
            }

            final TextView[] Jo = {(TextView) rootView.findViewById(R.id.JoA), (TextView) rootView.findViewById(R.id.JoB), (TextView) rootView.findViewById(R.id.JoC)};
            for (TextView i : Jo)
                i.setText("?");

            final TextView[] Si = {(TextView) rootView.findViewById(R.id.SiA), (TextView) rootView.findViewById(R.id.SiB), (TextView) rootView.findViewById(R.id.SiC)};
            for (TextView i : Si)
                i.setText("?");

            final TextView[] Re = {(TextView) rootView.findViewById(R.id.ReA), (TextView) rootView.findViewById(R.id.ReB), (TextView) rootView.findViewById(R.id.ReC)};
            for (TextView i : Re)
                i.setText("?");

            final TableRow[] Row = {(TableRow) rootView.findViewById(R.id.RowA), (TableRow) rootView.findViewById(R.id.RowB), (TableRow) rootView.findViewById(R.id.RowC)};

            for (int i = 0; i < NumPrinters; i++)
                if (s != null && s[i] != null) {
                    Jo[i].setText(String.valueOf(s[i][0]));
                    Si[i].setText(s[i][1] / 1024 + " KB");
                    if (s[i][2] == 1)
                        Re[i].setText("ready");
                    else if (s[i][2] == 2)
                        Re[i].setText("printing");
                    else
                        Re[i].setText("unknown");
                    Row[i].setBackgroundColor(badnessColor(s[i][0], s[i][1]));
                } else {
                    Jo[i].setText("!");
                    Si[i].setText("!");
                    Re[i].setText("!");
                }
            resetUpdating();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
////        public void onFragmentInteraction(Uri uri);
//    }

}
