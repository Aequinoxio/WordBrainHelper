package com.example.utente.wordbrainhelper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener {

    // Spinner element
    Spinner spinner;

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

        // Spinner element
        spinner = (Spinner) findViewById(R.id.spinner);
        // Spinner click listener
        spinner.setOnItemSelectedListener(this);

        // Loading spinner data from database
        loadSpinnerData();
    }

    /**
     * Function to load the spinner data from SQLite database
     * */
    private void loadSpinnerData() {
//        // database handler
//        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
//
//        // Spinner Drop down elements
//        List<String> lables = db.getAllLabels();
//
//        // Creating adapter for spinner
//        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_spinner_item, lables);
//
//        // Drop down layout style - list view with radio button
//        dataAdapter
//                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        DataAdapter mDbHelper = new DataAdapter(this);
        mDbHelper.createDatabase();
        mDbHelper.open();

       // Cursor testdata = mDbHelper.getCursor("SELECT * FROM livelli");

        List<String> lables=mDbHelper.getValues("SELECT DISTINCT livello FROM livelli ORDER BY livello asc");
        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, lables);

        // Drop down layout style - list view with radio button
        dataAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        mDbHelper.close();
    }

    public void searchClick(View v){
        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        String livello = spinner.getSelectedItem().toString();

        String lungParole= ((EditText) findViewById(R.id.txtLunghezzaParole)).getText().toString().trim();
        String sqlQuery= "SELECT lista_parole FROM parole_firma WHERE livello = '" + livello + "' ";
        if (lungParole.length() !=0){
            String RegExpAtom="["+lungParole+"]";
            String RegExp=RegExpAtom;
            for (int i=0;i<lungParole.length()-1;i++){
                RegExp = RegExp + "," + RegExpAtom;
            }
            sqlQuery += " AND lung_parole REGEXP '"+ RegExp+"' ";
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

        DataAdapter mDbHelper = new DataAdapter(this);
        mDbHelper.open();
        List<String> lables=mDbHelper.getValues(sqlQuery);
        int iParole=mDbHelper.getCursor(sqlQuery).getCount();

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, lables);

        ListView lstView = (ListView)findViewById(R.id.lstParole);
        //attaching data adapter to spinner
        lstView.setAdapter(dataAdapter);

        mDbHelper.close();

        TextView textView=(TextView)findViewById(R.id.textView4);
        textView.setText(getString(R.string.txtParole)+Integer.toString(iParole));
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
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }
}
