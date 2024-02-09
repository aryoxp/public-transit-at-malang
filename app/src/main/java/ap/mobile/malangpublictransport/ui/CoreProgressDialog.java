package ap.mobile.malangpublictransport.ui;

import android.content.Context;
import android.os.Build;
import android.widget.ProgressBar;
import android.widget.TextView;

import ap.mobile.malangpublictransport.R;

public class CoreProgressDialog extends CoreDialog {

  private int progress;
  private int max;

  CoreProgressDialog(Context context, String message) {
    super(context, message);
    this.progress = -1;
    this.max = 0;
  }

  public void setMinMax(int progress, int max) {
    this.progress = progress;
    this.max = max;
    ((ProgressBar) this.v.findViewById(R.id.pbDialogProgress)).setIndeterminate(progress == -1);
    ((ProgressBar) this.v.findViewById(R.id.pbDialogProgress)).setMax(max);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      ((ProgressBar) this.v.findViewById(R.id.pbDialogProgress)).setMin(0);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      ((ProgressBar) this.v.findViewById(R.id.pbDialogProgress)).setProgress(progress, true);
    } else ((ProgressBar) this.v.findViewById(R.id.pbDialogProgress)).setProgress(progress);
    ((TextView) this.v.findViewById(R.id.tvDialogProgressText)).setText(String.format("%d/%d", progress, max));
  }

  public void show(String message)  {
    ((TextView) this.v.findViewById(R.id.tvDialogProgressMessage)).setText(message);
    if (!this.dialog.isShowing())
      super.show();
  }

  public void setProgress(int progress, int max) {
    if (!this.dialog.isShowing())
      super.show();
    this.setMinMax(progress, max);
  }
}
