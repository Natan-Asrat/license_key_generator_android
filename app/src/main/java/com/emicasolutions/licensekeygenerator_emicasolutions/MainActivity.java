package com.emicasolutions.licensekeygenerator_emicasolutions;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private TextView licenseKeyTextView;
    private EditText secretKey;
    private Button copyButton;
    private Button verifyButton;
    private TextView resultLabel;
    private EditText codeInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        licenseKeyTextView = findViewById(R.id.license_key);
        secretKey = findViewById(R.id.secret_key_edit_text);
        codeInput = findViewById(R.id.code_edit_text);
        copyButton = findViewById(R.id.copyButton);
        verifyButton = findViewById(R.id.verifyButton);
        resultLabel = findViewById(R.id.resultLabel);

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(licenseKeyTextView.getText().toString());
            }
        });

        // Set Verify button action
        verifyButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                verifyUID();
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String hashUID(String uid, String secretKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = uid + secretKey;
            byte[] hash = digest.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("UID", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "UID copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    private String convertToHex(String uid) {
        // Convert the UID to an 8-character hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (char c : uid.toCharArray()) {
            hexString.append(String.format("%02x", (int) c));
        }
        return hexString.substring(0, Math.min(hexString.length(), 8));
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void verifyUID() {
        String code = codeInput.getText().toString();

        String licensekeyhash = hashUID(code, secretKey.getText().toString());
        String licensekeyHex = convertToHex(licensekeyhash);
        String[] codeParts = code.split("/");
        String type = codeParts[0];
        String dateIssued = codeParts[1];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            // Parse the date from the string
            LocalDate parsedDate = LocalDate.parse(dateIssued, formatter);
            LocalDate currentDate = LocalDate.now();

            boolean thirtyDaysPassed = parsedDate.plusDays(30).isBefore(currentDate);
            boolean NinetyDaysPassed = parsedDate.plusDays(90).isBefore(currentDate);
            boolean YearPassed = parsedDate.plusDays(365).isBefore(currentDate);
            boolean expirationCheck = true;
            switch (type){
                case "A":
                    expirationCheck = thirtyDaysPassed;
                    break;

                case "B":
                    expirationCheck = NinetyDaysPassed;
                    break;

                case "C":
                    expirationCheck = YearPassed;
                    break;

            }
            if(parsedDate.isAfter(currentDate)){
                resultLabel.setText("The date is ahead of your local time!");
                resultLabel.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                return;
            } else if (expirationCheck) {

                resultLabel.setText("Date expired!");
                resultLabel.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }else{
                licenseKeyTextView.setText(licensekeyHex);
                resultLabel.setText("Date is valid! Copy the key below.");
                resultLabel.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }

        } catch (DateTimeParseException e) {
            resultLabel.setText("Invalid date format!");
            resultLabel.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            return;
        }

    }
}