package ap.mobile.malangpublictransport.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import ap.mobile.malangpublictransport.R;

public class CoreDialog implements View.OnClickListener {
  protected String message;
  protected View v;
  protected Dialog dialog;

  @Override
  public void onClick(View view) {
    if(view.getId() == R.id.btDialogPositive && this.positiveListener != null) this.positiveListener.onPositive();
    if(view.getId() == R.id.btDialogNegative && this.negativeListener != null) this.negativeListener.onNegative();
  }

  public Dialog getDialog() {
    return dialog;
  }

  public interface OnPositiveListener {
    public void onPositive();
  }

  public interface OnNegativeListener {
    public void onNegative();
  }

  private OnPositiveListener positiveListener;
  private OnNegativeListener negativeListener;

  CoreDialog(Context context, String message) {
    View v = LayoutInflater.from(context).inflate(R.layout.dialog_plain, null, false);
    this.message = message;
    this.v = v;
    this.setPositive(new OnPositiveListener() {
      @Override
      public void onPositive() {
        dialog.dismiss();
      }
    });
  }

  public CoreDialog setContent(String message) {
    ((TextView) this.v.findViewById(R.id.tvDialogContent)).setText(message);
    return this;
  }

  public CoreDialog setPositive(OnPositiveListener positiveListener) {
    this.positiveListener = positiveListener;
    return this;
  }
  public CoreDialog setPositive(OnPositiveListener positiveListener, String label) {
    this.positiveListener = positiveListener;
    ((TextView)this.v.findViewById(R.id.btDialogPositive)).setText(label);

    return this;
  }

  public CoreDialog setNegative(OnNegativeListener negativeListener) {
    this.negativeListener = negativeListener;
    return this;
  }

  public CoreDialog setNegative(OnNegativeListener negativeListener, String label) {
    this.negativeListener = negativeListener;
    ((TextView)this.v.findViewById(R.id.btDialogNegative)).setText(label);
    return this;
  }

  public CoreDialog build() {
    this.dialog = new Dialog(this.v.getContext());
    this.dialog.setContentView(this.v);
    if (this.negativeListener == null) v.findViewById(R.id.btDialogNegative).setVisibility(View.GONE);
    else {
      v.findViewById(R.id.btDialogNegative).setVisibility(View.VISIBLE);
      v.findViewById(R.id.btDialogNegative).setOnClickListener(this);
    }
    v.findViewById(R.id.btDialogPositive).setOnClickListener(this);
    return this;
  }

  public Dialog show() {
    if (this.dialog == null)
      this.build();
    TextView tv = this.v.findViewById(R.id.tvDialogContent);
    if (tv != null) tv.setText(message);
    this.dialog.show();
    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
    lp.copyFrom(this.dialog.getWindow().getAttributes());
    lp.width = (int) dpToPx(this.v.getContext(), 350);
    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
    this.dialog.getWindow().setAttributes(lp);
    return this.dialog;
  }

  public static float spToPx(Context ctx,float sp){
    return sp * ctx.getResources().getDisplayMetrics().scaledDensity;
  }

  public static float pxToDp(final Context context, final float px) {
    return px / context.getResources().getDisplayMetrics().density;
  }

  public static float dpToPx(final Context context, final float dp) {
    return dp * context.getResources().getDisplayMetrics().density;
  }

}
