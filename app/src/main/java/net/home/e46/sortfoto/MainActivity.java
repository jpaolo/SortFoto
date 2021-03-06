package net.home.e46.sortfoto;

import android.content.UriPermission;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final String SORT_FOTO = "SortFoto";
    private Button sortButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sortButton = (Button) findViewById(R.id.sort_button);
        sortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dcmiLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
                // dirty workaround for external sd card
                dcmiLocation = dcmiLocation.replace("emulated/0", "extSdCard");
                String cameraImageLocation = dcmiLocation + "/Camera";
                Log.d(SORT_FOTO, "cameraImageLocation=" + cameraImageLocation);
                File dir = new File(cameraImageLocation);
                int count = 0;
                for (File f : dir.listFiles()) {
                    Log.d("file", f.getName());
                    if (!f.isDirectory()) {
                        try {
                            ExifInterface info = new ExifInterface(f.getAbsolutePath());
                            if (info != null) {
                                String dateTime = info.getAttribute(ExifInterface.TAG_DATETIME);
                                if (dateTime != null) {
                                    Log.d("TAG_DATETIME", dateTime);
                                    ImageDate imageDate = parseDateTimeTag(dateTime);
                                    String path = cameraImageLocation + "/" + imageDate.getYear() + "-" + imageDate.getQuarter();

                                    File[] dirs= ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
                                    Log.e("[0]", dirs[0].getAbsolutePath());
                                    Log.e("[1]", dirs[1].getAbsolutePath());
                                    path = dirs[1].getAbsolutePath() + "/../../../../DCIM/Camera" + "/" + imageDate.getYear() + "-" + imageDate.getQuarter();


                                    Log.e("path", path);
//                                    String path1 = Environment.getExternalStorageDirectory().getPath();
//                                    String path2 = getApplicationContext().getFilesDir().getPath();
//                                    Log.e("path1", path1);
//                                    Log.e("path2", path2);
//                                    String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
//                                    Log.e("root", root);
//
                                    File qDir = new File(path);
                                    Log.e("real path=", qDir.getCanonicalPath());
                                    boolean newDirCreated = false;
                                    if (!qDir.exists()) {
                                        Log.e(SORT_FOTO, "trying to make " + qDir.getAbsolutePath());
//                                        // dirty workaround for external sd card
//                                        MediaFile mDir = new MediaFile(getApplicationContext().getContentResolver(), qDir);
                                        newDirCreated = qDir.mkdirs();
                                        Log.e("dir creation", qDir.getName() + " is " + (newDirCreated? "" : "NOT ") + "created!");
//                                        newDirCreated = qDir.mkdirs();
                                    } else if (!qDir.isDirectory()) {
                                        // TODO: enhance later - create a dir with a different name
                                        Log.e("mkqdir", "existing file name conflicts with " + qDir.getName());
                                        return;
                                    }
                                    if (newDirCreated) {
                                        Log.i(SORT_FOTO, "moving " + f.getName() + " to " + qDir.getName());
                                        boolean moved = f.renameTo(new File(qDir.getAbsoluteFile() + "/" + f.getName()));
                                        if (moved) {
                                            count++;
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;  // temp code for debugging
                }
                Log.i(SORT_FOTO, "moved " + count + " images!");

                List<UriPermission> permissions = getBaseContext().getContentResolver().getPersistedUriPermissions();
                Log.e("permissions count", String.valueOf(permissions.size()));
                for (UriPermission permission : permissions) {
                    Log.e("permission", permission.toString());
                    if (permission.isWritePermission()) {
                        Log.e(permission.toString(), "writable");
                    }
                }

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ImageDate parseDateTimeTag(String s) {
        ImageDate imageDate = null;
        int year;
        int month;

        if (s.split(" ").length == 2)  {
            s = s.split(" ")[0];
            if (s.split(":").length == 3) {
                try {
                    year = Integer.parseInt(s.split(":")[0]);
                    month = Integer.parseInt(s.split(":")[1]);
                    imageDate = new ImageDate(year, month);
                } catch (NumberFormatException e) {
                    // log below
                }
            }
        }

        if (imageDate == null) {
            Log.e("parseDateTimeTag()", "Failed to parse TAG_DATETIME");
        }

        return imageDate;
    }

    private class ImageDate {
        private int year;
        private int month;
        private Quarter quarter;

        public ImageDate(int year, int month) {
            if (month>0 && month<13) {
                this.month = month;
                this.quarter = Quarter.parse(this.month);
            } else {
                throw new InvalidParameterException("month must be between 1 and 12");
            }

            this.year = year;
        }

        public int getMonth() {
            return month;
        }
        public int getYear() {
            return year;
        }
        public String getQuarter() {
            return quarter.name();
        }
    }

    enum Quarter {
        UNDEFINED,
        Q1,
        Q2,
        Q3,
        Q4;

        public static Quarter parse(int month) {
            try {
                return Quarter.values()[((int) Math.ceil(month/3.0))];
            } catch (IllegalArgumentException | NullPointerException e) {
                return UNDEFINED;
            }
        }
    }
}
