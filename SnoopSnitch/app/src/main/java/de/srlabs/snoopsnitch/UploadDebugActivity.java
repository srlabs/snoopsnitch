package de.srlabs.snoopsnitch;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import de.srlabs.snoopsnitch.qdmon.EncryptedFileWriter;
import de.srlabs.snoopsnitch.upload.DumpFile;
import de.srlabs.snoopsnitch.util.MsdConfig;
import de.srlabs.snoopsnitch.util.MsdDatabaseManager;
import de.srlabs.snoopsnitch.util.MsdDialog;
import de.srlabs.snoopsnitch.util.MsdLog;
import de.srlabs.snoopsnitch.util.Utils;

public class UploadDebugActivity extends BaseActivity {
    private Button btnDebugUpload;
    private Button btnDebugCancel;
    private CheckBox checkDebugUploadDatabaseMetadata;
    private CheckBox checkDebugUploadRadioTraces;
    private CheckBox checkDebugUploadSnoopsnitchDebugLogs;
    private EditText editTextUploadDebugWhatToReport;
    private EditText editTextUploadDebugContactInfo;
    private boolean noContactInfoConfirmed = false;
    private boolean noReportTextConfirmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_debug);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        this.btnDebugUpload = (Button) findViewById(R.id.btnDebugUpload);
        this.btnDebugCancel = (Button) findViewById(R.id.btnDebugCancel);
        this.checkDebugUploadDatabaseMetadata = (CheckBox) findViewById(R.id.checkDebugUploadDatabaseMetadata);
        this.checkDebugUploadRadioTraces = (CheckBox) findViewById(R.id.checkDebugUploadRadioTraces);
        this.checkDebugUploadSnoopsnitchDebugLogs = (CheckBox) findViewById(R.id.checkDebugUploadSnoopsnitchDebugLogs);
        this.btnDebugCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        this.btnDebugUpload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doUpload();
            }
        });
        this.editTextUploadDebugWhatToReport = (EditText) findViewById(R.id.editTextUploadDebugWhatToReport);
        this.editTextUploadDebugContactInfo = (EditText) findViewById(R.id.editTextUploadDebugContactInfo);
    }

    private void doUpload() {
        boolean uploadDatabaseMetadata = checkDebugUploadDatabaseMetadata.isChecked();
        boolean uploadRadioTraces = checkDebugUploadRadioTraces.isChecked();
        boolean uploadSnoopsnitchDebugLogs = checkDebugUploadSnoopsnitchDebugLogs.isChecked();
        /*if (!uploadDatabaseMetadata && !uploadRadioTraces && !uploadSnoopsnitchDebugLogs) {
            MsdDialog.makeNotificationDialog(this, getString(R.string.upload_debug_please_select), null, true).show();
            return;
        }*/
        String whatToReport = this.editTextUploadDebugWhatToReport.getText().toString().trim();
        String contactInfo = this.editTextUploadDebugContactInfo.getText().toString().trim();
        if (!noContactInfoConfirmed && !isValidEmail(contactInfo)) {
            MsdDialog.makeConfirmationDialog(this, getString(R.string.upload_debug_confirm_no_contact), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    noContactInfoConfirmed = true;
                    doUpload();
                }
            }, null, true).show();
            return;
        }
        if (!noReportTextConfirmed && whatToReport.length() < 10) {
            MsdDialog.makeConfirmationDialog(this, getString(R.string.upload_debug_confirm_no_description), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    noReportTextConfirmed = true;
                    doUpload();
                }
            }, null, true).show();
            return;
        }
        Vector<DumpFile> files = new Vector<DumpFile>();
        try {
            SQLiteDatabase db = MsdDatabaseManager.getInstance().openDatabase();
            if (uploadRadioTraces) {
                Vector<DumpFile> dumpFiles = DumpFile.getFiles(db, DumpFile.TYPE_ENCRYPTED_QDMON, System.currentTimeMillis() - 3600 * 1000L, System.currentTimeMillis(), 0);
                if(dumpFiles != null && dumpFiles.size() > 0) {
                    for (DumpFile df : dumpFiles) {
                        files.add(df);
                        df.markForUpload(db);
                    }
                }
            }
            if (uploadSnoopsnitchDebugLogs) {
                long debugLogId = getMsdServiceHelperCreator().getMsdServiceHelper().reopenAndUploadDebugLog();
                DumpFile df = DumpFile.get(db, debugLogId);
                if(df != null) {
                    df.markForUpload(db);
                    files.add(df);
                }
            }
            if (uploadDatabaseMetadata) {
                DumpFile dbMetaData = Utils.uploadMetadata(this, db, null, System.currentTimeMillis(), System.currentTimeMillis(), "meta-suspicious-");
                if(dbMetaData != null)
                    files.add(dbMetaData);
            }
            String json = "{\n\"APPID\":\"" + MsdConfig.getAppId(this) + "\",\n";
            json += "\"REPORT_CONTACT\":" + escape(contactInfo) + ",\n";
            json += "\"REPORT_TEXT\":" + escape(whatToReport) + ",\n";
            json += "\"SNOOPSNITCH_VERSION\":" + escape(BuildConfig.VERSION_NAME) + ",\n";
            json += "\"REPORT_FILES\": [ ";
            for (int i = 0; i < files.size(); i++) {
                DumpFile df = files.get(i);
                df.markForUpload(db);
                json += "\"" + df.getFilename() + "\"";
                if (i < files.size() - 1) {
                    json += ", ";
                }
            }
            json += " ]\n}";
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            // Calendar.MONTH starts counting with 0
            String fileName = String.format(Locale.US, "bugreport_%04d-%02d-%02d_%02d-%02d-%02dUTC.gz", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
            EncryptedFileWriter outputFile = new EncryptedFileWriter(this, fileName + ".smime", true, fileName, false);

            outputFile.write(json);
            outputFile.close();

            DumpFile df = new DumpFile(outputFile.getEncryptedFilename(), DumpFile.TYPE_BUG_REPORT, System.currentTimeMillis(), System.currentTimeMillis());
            df.recordingStopped();
            df.insert(db);
            df.markForUpload(db);
            getMsdServiceHelperCreator().getMsdServiceHelper().triggerUploading();
            String reportId = df.getReportId();
            MsdDialog.makeNotificationDialog(this, String.format(getString(R.string.upload_debug_confirmation_msg), reportId), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }, false).show();
        } catch (Exception e) {
            MsdLog.e("UploadDebugActivity", "Exception while preparing debug logs to upload", e);
            MsdDialog.makeNotificationDialog(this, "Exception while preparing debug logs to upload: " + e.getMessage(), null, true).show();
        } finally {
            MsdDatabaseManager.getInstance().closeDatabase();
        }
    }

    private String escape(String input) {
        if (input == null)
            return "undefined";
        return "\"" + input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

}
