package com.example.utente.wordbrainhelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener, AdapterView.OnItemLongClickListener {

    // Spinner element
    Spinner spinner;
    String RowLongSelected="";
    DataAdapter mDbHelper=null;
    boolean soloNonRisolti=false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            String s = getString(R.string.app_name) +" - Versione " + BuildConfig.VERSION_NAME +"\n";
            s+="by "+ getString(R.string.Autore)+"\n\n";
            s+=getString(R.string.app_desc);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.action_about)
                    .setMessage(s)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CheckedTextView ctv = (CheckedTextView) findViewById(R.id.checkedTextView);

        ctv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctv.toggle();
                soloNonRisolti=ctv.isChecked();
                updateListView();
            }
        });

        // Aggiorno il DB creandolo se necessario
        mDbHelper = new DataAdapter(this);
        mDbHelper.createDatabase();
        // Spinner element
        spinner = (Spinner) findViewById(R.id.spinner);
        // Spinner click listener
        spinner.setOnItemSelectedListener(this);

        // Loading spinner data from database
        loadSpinnerData();

        ListView lstView = (ListView)findViewById(R.id.lstParole);
        lstView.setOnItemLongClickListener(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        //Log.v("long clicked", "pos: " + position + parent.getItemAtPosition(position).toString());

        RowLongSelected=parent.getItemAtPosition(position).toString();
        //String dummy=" \\(Già risolto\\)";
        String dummy=getString(R.string.giaRisoltoRegexp);
        RowLongSelected = RowLongSelected.replaceAll(dummy, "").trim();
        /*
        view.setBackgroundColor(Color.GRAY);
        view.refreshDrawableState();
*/
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked

                        mDbHelper.open();

                        // Aggiorno (flip) dello stato risolto)
                        mDbHelper.setValues("UPDATE parole_firma " +
                                "SET risolto = CASE WHEN risolto=1 THEN 0 ELSE 1 END " +
                                "WHERE lista_parole='"+RowLongSelected+"'");

                        mDbHelper.close();

                        updateListView();

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Cambio stato (risolto / non risolto)?").setPositiveButton("Si", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

        return false;
    }


    /**
     * Function to load the spinner data from SQLite database
     * */
    private void loadSpinnerData() {

        mDbHelper.open();

       // Cursor testdata = mDbHelper.getCursor("SELECT * FROM livelli");

        List<String> lables=mDbHelper.getValues("SELECT DISTINCT livello FROM livelli ORDER BY livello asc",0);
        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, lables);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        mDbHelper.close();
    }

    public void searchClick(View v){
        updateListView();
    }

    public void updateListView(){
        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        String livello = spinner.getSelectedItem().toString();

        String lungParole= ((EditText) findViewById(R.id.txtLunghezzaParole)).getText().toString().trim();
        String sqlQuery= "SELECT lung_parole, lista_parole || CASE WHEN risolto=1 THEN ' (Già risolto)' ELSE '' END FROM parole_firma WHERE livello = '" + livello + "' ";
//
        // La Regexp nella query SQL Ha funzionato fino alla versione 1.3. Ora la faccio diversa e seleziono la
        // le stringhe applicando la REGEXP a mano
        String RegExp="";
        if (lungParole.length() !=0){
            String RegExpAtom="["+lungParole+"]";
            RegExp="^"+RegExpAtom;
            for (int i=0;i<lungParole.length()-1;i++){
                RegExp = RegExp + "," + RegExpAtom;
            }
//            sqlQuery += " AND lung_parole REGEXP '"+ RegExp+"' ";
            RegExp+="$";
        }

        if (lungParole.trim().length() !=0){
            sqlQuery += " AND num_parole = '"+lungParole.trim().length()+"' ";
        }

        if (soloNonRisolti){
            sqlQuery += " AND risolto=0 ";
        }

        String lettereRompicapo= ((EditText) findViewById(R.id.txtLettereRompicapo)).getText().toString().trim();

        // Ordino le lettere del rompicapo e le aggiungo alla query
        if (lettereRompicapo.length()!=0){
            lettereRompicapo = lettereRompicapo.toUpperCase();
            char[] dummy=lettereRompicapo.toCharArray();
            Arrays.sort(dummy);
            lettereRompicapo=String.valueOf(dummy);
            sqlQuery += " AND firma_ordinata='"+lettereRompicapo+"'";
        }

        mDbHelper.open();
 //       List<String> lables_TEMP=mDbHelper.getValues(sqlQuery);
        Cursor cursor=mDbHelper.getCursor(sqlQuery);
        List<String> lables=new ArrayList<String>();
        int iParole=0; // Parole trovate

        if (RegExp.length()!=0) {
            //Iterator<String> itr = lables_TEMP.iterator();
            Pattern ptrn = Pattern.compile(RegExp);
            String temp;
            Matcher m;

            if (cursor.moveToFirst()) {
                do {
                    temp = cursor.getString(cursor.getColumnIndex("lung_parole"));
                    m = ptrn.matcher(temp);
                    if (m.find()) {
                        lables.add(cursor.getString(1));
                        iParole++;
                    }
                } while (cursor.moveToNext());
            }
        } else {
            lables=mDbHelper.getValues(sqlQuery, 1);;
            iParole=mDbHelper.getCursor(sqlQuery).getCount();
        }

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, lables);

        ListView lstView = (ListView)findViewById(R.id.lstParole);
        //attaching data adapter to spinner
        lstView.setAdapter(dataAdapter);

        mDbHelper.close();

        TextView textView = (TextView) findViewById(R.id.textView4);
        textView.setText(getString(R.string.txtParole)+Integer.toString(iParole));

        // Chiudo la tastiera
        View view = getCurrentFocus();

        if(view!=null){
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
        // On selecting a spinner item
        String label = parent.getItemAtPosition(position).toString();

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Livello selezionato: " + label,
                Toast.LENGTH_LONG).show();

        // Cancello le parole trovate
        ListView lstParole = (ListView)findViewById(R.id.lstParole);

        // Reset controlli interfaccia grafica
        lstParole.setAdapter(null);
        TextView textView=(TextView)findViewById(R.id.textView4);
        textView.setText(getString(R.string.txtParole));

        updateListView();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }
}
