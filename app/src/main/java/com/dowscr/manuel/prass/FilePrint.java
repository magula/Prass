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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FilePrint#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FilePrint extends Fragment implements PrinterConstants {
    private static final AsyncTask[] PrintClasses = new AsyncTask[100];
    private static InputStream FileStream;
    private static int NumPrintJobs = 0;
    //    private OnFragmentInteractionListener mListener;
    private View rootView;
    private boolean PrintAll = true,
            DoubleSided = true,
            LongEdge = true,
            PDFPassNeeded = false;
    private int PPS = 1,
            PrinterID = 0,
            NC = 1;
    private String NP,
            PDFPass;
    private String FileName;

    public FilePrint() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static FilePrint newInstance() {
        FilePrint fragment = new FilePrint();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public static void cancelJob(int ID) {
        PrintClasses[ID].cancel(false);
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        if (intent.resolveActivity(getActivity().getPackageManager()) != null)
            startActivityForResult(intent, 42);
        else
            Toast.makeText(getActivity().getApplicationContext(), "File manager missing", Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 42 && resultCode == FragmentActivity.RESULT_OK) {
            handleData(data.getData());
        }
    }

    private String getContentName(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor == null)
            return null;
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        String name = null;
        if (nameIndex >= 0)
            name = cursor.getString(nameIndex);
        cursor.close();
        return name;
    }

    private void handleData(Uri U) {
        final TextView file = rootView.findViewById(R.id.file);

        ContentResolver resolver = getActivity().getContentResolver();

        String scheme = U.getScheme();
        if (Objects.requireNonNull(scheme).equals(ContentResolver.SCHEME_CONTENT)) {
            FileName = getContentName(resolver, U);
        } else if (scheme.equals(ContentResolver.SCHEME_FILE)) {
            FileName = U.getLastPathSegment();
        }
        file.setText(FileName);

        FileStream = null;
        try {
            FileStream = resolver.openInputStream(U);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        file.setText(FileName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (FileStream != null)
                FileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileStream = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootView = inflater.inflate(R.layout.fragment_file_print, container, false);

        Intent intent = getActivity().getIntent();
        String action = intent.getAction();
//        String scheme = intent.getScheme();

        if (Intent.ACTION_SEND.equals(action) /*&& type != null*/) {
//            if (scheme.equals("file")||scheme.equals("content")) {
            handleData((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
//            }
        }

        final View dummy = rootView.findViewById(R.id.dummy);
        final CheckBox printall = rootView.findViewById(R.id.printall);
        final CheckBox pdfpass_needed = rootView.findViewById(R.id.pdfpass);
        final CheckBox ds = rootView.findViewById(R.id.ds);
        final EditText np = rootView.findViewById(R.id.np);
        final RadioGroup pps = rootView.findViewById(R.id.pps);
        final RadioGroup edge = rootView.findViewById(R.id.edge);
        final RadioGroup pr = rootView.findViewById(R.id.Printer);
        final RadioButton le = rootView.findViewById(R.id.le);
        final RadioButton se = rootView.findViewById(R.id.se);
        final Button sf = rootView.findViewById(R.id.selectfile);
        final RadioButton pr1 = rootView.findViewById(R.id.Printer1);
        final RadioButton pr2 = rootView.findViewById(R.id.Printer2);
        final RadioButton pr3 = rootView.findViewById(R.id.Printer3);
        final EditText nc = rootView.findViewById(R.id.nc);
        final EditText pdfpwd = rootView.findViewById(R.id.pdfpwd);
        final Button ncdown = rootView.findViewById(R.id.ncdown);
        final Button ncup = rootView.findViewById(R.id.ncup);

        pr1.setText(Html.fromHtml(PrinterNames[0] + "<br><small>" + PrinterIDS[0] + "</small>"));
        pr2.setText(Html.fromHtml(PrinterNames[1] + "<br><small>" + PrinterIDS[1] + "</small>"));
        pr3.setText(Html.fromHtml(PrinterNames[2] + "<br><small>" + PrinterIDS[2] + "</small>"));

        sf.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        selectFile();
                    }
                }
        );

        printall.setOnClickListener(new
                                            View.OnClickListener() {
                                                public void onClick(View v) {
                                                    boolean on = ((CheckBox) v).isChecked();
                                                    if (!on) {
                                                        PrintAll = true;
                                                        np.setEnabled(false);
//                                                        np.setText("");
//                                                        rootView.requestFocus();
                                                    } else {
                                                        PrintAll = false;
                                                        np.setEnabled(true);
                                                        np.requestFocus();
                                                    }
                                                }
                                            }
        );
        pdfpass_needed.setOnClickListener(new
                                                  View.OnClickListener() {
                                                      public void onClick(View v) {
                                                          boolean on = ((CheckBox) v).isChecked();
                                                          if (!on) {
                                                              PDFPassNeeded = false;
                                                              pdfpwd.setEnabled(false);
//                                                        pdfpwd.setText("");
//                                                        rootView.requestFocus();
                                                          } else {
                                                              PDFPassNeeded = true;
                                                              pdfpwd.setEnabled(true);
                                                              pdfpwd.requestFocus();
                                                          }
                                                      }
                                                  }
        );
        ds.setOnCheckedChangeListener(new
                                              CompoundButton.OnCheckedChangeListener() {
                                                  public void onCheckedChanged(CompoundButton buttonView, boolean on) {
                                                      if (on) {
                                                          DoubleSided = true;
                                                          le.setEnabled(true);
                                                          se.setEnabled(true);
                                                      } else {
                                                          DoubleSided = false;
                                                          le.setEnabled(false);
                                                          se.setEnabled(false);
                                                          le.setClickable(true);
                                                          se.setClickable(true);
                                                      }
                                                  }
                                              }
        );
        pps.setOnCheckedChangeListener(new
                                               RadioGroup.OnCheckedChangeListener() {
                                                   public void onCheckedChanged(RadioGroup group, int checkedId) {
                                                       int i = 0;
                                                       switch (checkedId) {
                                                           case R.id.pps1:
                                                               i = 1;
                                                               break;
                                                           case R.id.pps2:
                                                               i = 2;
                                                               break;
                                                           case R.id.pps4:
                                                               i = 4;
                                                               break;
                                                           case R.id.pps8:
                                                               i = 8;
                                                               break;
                                                           case R.id.pps16:
                                                               i = 16;
                                                               break;
                                                       }
                                                       PPS = i;
                                                   }
                                               }
        );
        edge.setOnCheckedChangeListener(new
                                                RadioGroup.OnCheckedChangeListener() {
                                                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                                                        switch (checkedId) {
                                                            case R.id.se:
                                                                LongEdge = false;
                                                                break;
                                                            case R.id.le:
                                                                LongEdge = true;
                                                                break;
                                                        }
                                                    }
                                                }
        );
        nc.setText("1");
        ncdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int val = Integer.valueOf(nc.getText().toString());
                    if (val > 1)
                        nc.setText(Integer.toString(val - 1));
                } catch (NumberFormatException ignored) {
                }
            }
        });
        ncup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int val = Integer.valueOf(nc.getText().toString());
                    nc.setText(Integer.toString(val + 1));
                } catch (NumberFormatException ignored) {
                }
            }
        });
        pr.setOnCheckedChangeListener(new
                                              RadioGroup.OnCheckedChangeListener() {
                                                  public void onCheckedChanged(RadioGroup group, int checkedId) {
                                                      switch (checkedId) {
                                                          case R.id.Printer1:
                                                              PrinterID = 0;
                                                              break;
                                                          case R.id.Printer2:
                                                              PrinterID = 1;
                                                              break;
                                                          case R.id.Printer3:
                                                              PrinterID = 2;
                                                              break;
                                                      }
                                                  }
                                              }
        );

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
        inflater.inflate(R.menu.menu_print_file, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.print) {
            final EditText np = rootView.findViewById(R.id.np);
            final EditText nc = rootView.findViewById(R.id.nc);
            final EditText pdfpwd = rootView.findViewById(R.id.pdfpwd);
            if (FileStream == null) {
                Toast.makeText(getActivity().getApplicationContext(), "Filename missing", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (!PrintAll) {
                if (np.getText().toString().equals("")) {
                    Toast.makeText(getActivity().getApplicationContext(), "Range of pages to print missing.\nTo print all pages, please deselect the respective option.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                NP = np.getText().toString();
            }
            if (PDFPassNeeded) {
                if (pdfpwd.getText().toString().equals("")) {
                    Toast.makeText(getActivity().getApplicationContext(), "PDF password missing.\nTo print an unencrypted PDF-file, please deselect the respective option.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                PDFPass = pdfpwd.getText().toString();
            }
            NC = Integer.valueOf(nc.getText().toString());

            FireMissilesDialogFragment dia = new FireMissilesDialogFragment();

            Bundle args = new Bundle();
            args.putBoolean("PrintAll", PrintAll);
            args.putBoolean("DoubleSided", DoubleSided);
            args.putBoolean("LongEdge", LongEdge);
            args.putBoolean("PDFPassNeeded", PDFPassNeeded);
            args.putInt("PPS", PPS);
            args.putInt("PrinterID", PrinterID);
            args.putInt("NC", NC);
            args.putString("NP", NP);
            args.putString("PDFPass", PDFPass);
            args.putString("FileName", FileName);

            dia.setArguments(args);

            dia.show(getActivity().getSupportFragmentManager(), "missiles.");
        }

        return super.onOptionsItemSelected(item);
    }

    public static class FireMissilesDialogFragment extends DialogFragment {
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            boolean PrintAll = getArguments().getBoolean("PrintAll"),
                    DoubleSided = getArguments().getBoolean("DoubleSided"),
                    LongEdge = getArguments().getBoolean("LongEdge"),
                    PDFPassNeeded = getArguments().getBoolean("PDFPassNeeded");
            final int PPS = getArguments().getInt("PPS"),
                    PrinterID = getArguments().getInt("PrinterID"),
                    NC = getArguments().getInt("NC");
            String NP = getArguments().getString("NP"),
                    PDFPass = getArguments().getString("PDFPass");
            //TODO Uri File should be given as an argument
//            Uri File = getArguments().getUri("FileName");
            final String FileName = getArguments().getString("FileName");
            final String commandoPrint;
            final String TS = String.valueOf(System.currentTimeMillis());

            commandoPrint = (PDFPassNeeded ? "qpdf --password=" + PDFPass + " --decrypt \"" + TS + FileName + "\" \"" + TS + FileName + "\" && " : "")
                    + "lp -d " + PrinterIDS[PrinterID]
                    + " -o PageRegion=A4"
                    + " -o sides=" + (!DoubleSided ? ("one-sided") : ("two-sided" + (LongEdge ? ("-long-edge") : ("-short-edge")) + " -o Duplex=DuplexNoTumble -o HPOption_Duplexer=True"))
                    + " -o number-up" + "=" + (PPS)
                    + " -o number-up-layout=tblr"
                    + (PrintAll ? "" : " -P " + NP)
                    + " -o Collate=True"
                    + " -n " + NC
                    + " \"" + TS + FileName + "\"";
            Log.i("PrintCommando", commandoPrint);
            builder.setMessage("Print " + FileName + " on " + PrinterNames[PrinterID] + "?")
                    .setPositiveButton("FIRE", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            int ID = NumPrintJobs++;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                PrintClasses[ID] = new Print(getActivity(), FileName, FileStream, PrinterID, commandoPrint, TS, ID).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            } else {
                                PrintClasses[ID] = new Print(getActivity(), FileName, FileStream, PrinterID, commandoPrint, TS, ID).execute();
                            }
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
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
