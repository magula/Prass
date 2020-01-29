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
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.dowscr.manuel.prass.MainActivity.NumPrinters;
import static com.dowscr.manuel.prass.MainActivity.PrinterIds;
import static com.dowscr.manuel.prass.MainActivity.PrinterNames;
import static java.lang.Math.max;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Statistics#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Statistics extends Fragment {

    //    private OnFragmentInteractionListener mListener;
    private View rootView;
    private LayoutInflater stinflater;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ArrayList<PrinterRow> Rows = new ArrayList<>();

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

    //    A table row, containing TextViews for a printer's name,
//    its number of running jobs, the size of the running jobs,
//    and its status.
    private class PrinterRow {
        TableRow row;
        TextView PrinterDescription,
                PrinterNumberofJobs,
                PrinterSizeofJobs,
                PrinterStatus;

        PrinterRow(Context context, String PrinterName) {
            row = new TableRow(context);

            PrinterDescription = new TextView(context);
            PrinterNumberofJobs = new TextView(context);
            PrinterSizeofJobs = new TextView(context);
            PrinterStatus = new TextView(context);

            TextView[] Cells = {PrinterDescription, PrinterNumberofJobs, PrinterSizeofJobs, PrinterStatus};
            for (int i = 0; i < Cells.length; i++) {
                Cells[i].setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                Cells[i].setLayoutParams(new TableRow.LayoutParams(i));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    Cells[i].setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                else
                    Cells[i].setTextAppearance(context, R.style.TextAppearance_AppCompat_Medium);
                row.addView(Cells[i]);
            }

            PrinterDescription.setText(PrinterName);
            reset();
        }

        void setInfo(String info) {
            row.setBackgroundColor(Color.TRANSPARENT);
            PrinterNumberofJobs.setText(info);
            PrinterSizeofJobs.setText(info);
            PrinterStatus.setText(info);
        }

        void reset() {
            setInfo("?");
        }

        void err() {
            setInfo("!");
        }

        void think() {
            setInfo(".");
        }

        void setData(String Jo, String Si, String Re) {
            PrinterNumberofJobs.setText(Jo);
            PrinterSizeofJobs.setText(Si);
            PrinterStatus.setText(Re);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootView = inflater.inflate(R.layout.fragment_statistics, container, false);

        mSwipeRefreshLayout = rootView.findViewById(R.id.swiperefresh);

        updatePrinters();

        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        updateStatistics();
                    }
                }
        );

        updateStatistics();

        stinflater = inflater;
        // Inflate the layout for this fragment
        return rootView;
    }

    public void updatePrinters() {
        TableLayout table = rootView.findViewById(R.id.workloads);
        for (PrinterRow r : Rows)
            table.removeView(r.row);
        Rows.clear();
        for (String Name : PrinterNames) {
            PrinterRow row = new PrinterRow(this.getContext(), Name);
            table.addView(row.row);
            Rows.add(row);
        }
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
        //TODO Don't call this every time, just when necessary.
        updatePrinters();

        final TextView TV = rootView.findViewById(R.id.tv);
        final TextView EB = rootView.findViewById(R.id.errbox);

        try {
            for (PrinterRow i : Rows)
                i.think();
            TV.setText("...");
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
                String commandquota = MainActivity.getCommandQuota();
                String command = commandquota;
                if (commandquota.length() == 0)
                    command += "echo \"Dummy Quota\"";
                //To speed up usage of ssh, multiple commands are concatenated
                //and sent at once. Result will be separated by string "separator"
                final String separator = "64251674245076664132";
                for (int i = 0; i < NumPrinters; i++)
                    command += "&&echo -n \"" + separator + "\"&&" + "lpq -P " + PrinterIds.get(i);
                Log.i("Workload command", command);
                String Result = ConnectionHandler.executeRemoteCommand(command);
                String[] listofqueues = Result.split(separator);

//                Extract printquota from result
                if (commandquota.length() != 0) {
                    int printquota = Integer.parseInt((listofqueues[0].split("\\s+"))[6]);
                    S[NumPrinters][0] = printquota;
                } else
                    S[NumPrinters][0] = -1;


//                Extract stats on single printers from result
                for (int pr = 0; pr < NumPrinters; pr++) {
                    try {
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
                    } catch (Exception e) {
                        S[pr] = null;
                    }
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
                final TextView EB = rootView.findViewById(R.id.errbox);
                EB.setText(Ex);
                EB.setVisibility(View.VISIBLE);
            }

            final TextView TV = rootView.findViewById(R.id.tv);
            if (s == null || s[NumPrinters] == null)
                TV.setText("ERR");
            else {
                int printquota = s[NumPrinters][0];
                if (printquota != -1)
                    TV.setText(String.valueOf(printquota));
                else
                    TV.setText("[unavailable]");
            }

            for (PrinterRow i : Rows)
                i.reset();

            for (int i = 0; i < NumPrinters; i++)
                if (s != null && s[i] != null) {
                    String Jo = String.valueOf(s[i][0]),
                            Si = s[i][1] / 1024 + " KB",
                            Re;
                    if (s[i][2] == 1)
                        Re = "ready";
                    else if (s[i][2] == 2)
                        Re = "printing";
                    else
                        Re = "unknown";
                    Rows.get(i).setData(Jo, Si, Re);
                    Rows.get(i).row.setBackgroundColor(badnessColor(s[i][0], s[i][1]));
                } else {
                    Rows.get(i).err();
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
