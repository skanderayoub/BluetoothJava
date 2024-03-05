package com.example.bluetoothjava;

import android.content.Context;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVParser {

    private Context context;

    public CSVParser(Context context) {
        this.context = context;
    }

    public String[][] parseCSV(String fileName) throws IOException {
        List<String[]> data = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(context.getAssets().open(fileName)))) {
            data = reader.readAll();
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
        return data.toArray(new String[data.size()][]);
    }

    public void populateTable(ArrayList<String[]> data, TableLayout table) {
        table.removeAllViews(); // Clear existing data

        // Create table header row
        TableRow headerRow = new TableRow(context);
        for (String header : data.get(0)) {
            TextView headerText = new TextView(context);
            headerText.setText(header);
            headerText.setTextAppearance(context, android.R.style.TextAppearance_Holo_Small_Inverse);
            headerText.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)); // Set width to wrap_content
            headerRow.addView(headerText);
        }
        table.addView(headerRow);

        // Add data rows
        for (int i = 1; i < data.size(); i++) {
            TableRow dataRow = new TableRow(context);
            for (String cellValue : data.get(i)) {
                TextView cellText = new TextView(context);
                cellText.setText(cellValue);
                cellText.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)); // Set width to wrap_content
                dataRow.addView(cellText);
            }
            table.addView(dataRow);
        }
    }
}
