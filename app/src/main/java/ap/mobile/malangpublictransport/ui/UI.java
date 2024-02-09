package ap.mobile.malangpublictransport.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.zip.Inflater;

import ap.mobile.malangpublictransport.R;


public class UI {

  public static CoreDialog dialog(Context context, String message) {
    CoreDialog d = new CoreDialog(context, message);
    return d;
  }

  public static CoreProgressDialog progress(Context context, boolean indeterminate, int max, String message) {
    CoreProgressDialog d = new CoreProgressDialog(context, message);
    d.v = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null, false);
    d.build();
    ((TextView)d.v.findViewById(R.id.tvDialogProgressMessage)).setText(message);
    ((TextView)d.v.findViewById(R.id.tvDialogProgressText)).setText(indeterminate ? "" : "0/" + max);
    ((ProgressBar)d.v.findViewById(R.id.pbDialogProgress)).setIndeterminate(indeterminate);
    return d;
  }

}
