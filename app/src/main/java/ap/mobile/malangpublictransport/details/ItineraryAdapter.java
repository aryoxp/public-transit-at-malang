package ap.mobile.malangpublictransport.details;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ap.mobile.malangpublictransport.R;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {

    private List<Itinerary.Step> steps;

    public ItineraryAdapter(ArrayList<Itinerary.Step> steps) {
        this.steps = steps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch(viewType) {
            case 1:
                return new StartViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_startpoint, parent, false
                ));
            case 2:
                return new WalkViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_walk, parent, false
                ));
            case 3:
                return new LineViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_line, parent, false
                ));
            case 4:
                return new WalkTransferViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_walk_interchange, parent, false
                ));
            default:
                return new EndViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_endpoint, parent, false
                ));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 1:
                StartViewHolder svh = (StartViewHolder) holder;
                svh.bind((Itinerary.StartStep) this.steps.get(position));
                break;
            case 2:
                WalkViewHolder wvh = (WalkViewHolder) holder;
                wvh.bind((Itinerary.WalkStep) this.steps.get(position));
                break;
            case 3:
                LineViewHolder lvh = (LineViewHolder) holder;
                lvh.bind((Itinerary.LineStep) this.steps.get(position));
                break;
            case 4:
                WalkTransferViewHolder tvh = (WalkTransferViewHolder) holder;
                tvh.bind((Itinerary.WalkTransferStep) this.steps.get(position));
                break;
            case 5:
                EndViewHolder evh = (EndViewHolder) holder;
                evh.bind((Itinerary.EndStep) this.steps.get(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(steps.get(position) instanceof Itinerary.StartStep) return 1;
        if(steps.get(position) instanceof Itinerary.WalkStep) return 2;
        if(steps.get(position) instanceof Itinerary.LineStep) return 3;
        if(steps.get(position) instanceof Itinerary.WalkTransferStep) return 4;
        if(steps.get(position) instanceof Itinerary.EndStep) return 5;
        return 0;
    }

    @Override
    public int getItemCount() {
        return this.steps.size();
    }

    public void setSteps(List<Itinerary.Step> steps) {
        this.steps = steps;
    }
    public List<Itinerary.Step> getSteps() { return this.steps; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class StartViewHolder extends ViewHolder {

        private TextView tvLocation;
        private TextView tvLabel;

        StartViewHolder(View itemView) {
            super(itemView);
            this.tvLabel = itemView.findViewById(R.id.tv_start_label);
            this.tvLocation = itemView.findViewById(R.id.tv_start_location);
        }

        void bind(Itinerary.StartStep step) {
            this.tvLocation.setText(step.getLocation());
            this.tvLabel.setText(step.getLabel());
        }

    }

    public static class WalkViewHolder extends ViewHolder {

        private TextView tvLabel;

        WalkViewHolder(View itemView) {
            super(itemView);
            this.tvLabel = itemView.findViewById(R.id.tv_walk_label);
        }

        void bind(Itinerary.WalkStep step) {
            this.tvLabel.setText(step.getLabel());
        }
    }

    public static class LineViewHolder extends ViewHolder {

        private TextView tvStartPoint;
        private TextView tvLineName;
        private TextView tvDistance;
        private TextView tvPrice;
        private TextView tvEndPoint;
        private View vLineBar;
        private ImageView ivLineIcon;


        LineViewHolder(View itemView) {
            super(itemView);
            this.tvStartPoint = itemView.findViewById(R.id.tv_start_point);
            this.tvLineName = itemView.findViewById(R.id.tv_line_name);
            this.tvDistance = itemView.findViewById(R.id.tv_distance);
            this.tvPrice = itemView.findViewById(R.id.tv_price);
            this.tvEndPoint = itemView.findViewById(R.id.tv_end_point);
            this.vLineBar = itemView.findViewById(R.id.route_line_bar);
            this.ivLineIcon = itemView.findViewById(R.id.route_line_icon);
        }

        void bind(Itinerary.LineStep step) {
            this.tvStartPoint.setText(step.getStartPoint());
            this.tvLineName.setText(step.getLineName());

            this.tvDistance.setText(step.getDistance());
            this.tvPrice.setText(step.getPrice());
            this.tvEndPoint.setText(step.getEndPoint());
            ViewCompat.setBackgroundTintList(
                    this.vLineBar,
                    ColorStateList.valueOf(step.getColor()));
            this.ivLineIcon.setColorFilter(step.getColor());
            this.tvLineName.setBackgroundTintList(
                    ColorStateList.valueOf(step.getColor()));
        }
    }

    public static class WalkTransferViewHolder extends ViewHolder {

        private TextView tvLabel;

        WalkTransferViewHolder(View itemView) {
            super(itemView);
            this.tvLabel = itemView.findViewById(R.id.tv_transfer_label);
        }

        void bind(Itinerary.WalkTransferStep step) {
            this.tvLabel.setText(step.getLabel());
        }
    }

    public static class EndViewHolder extends ViewHolder {

        private TextView tvLocation;
        private TextView tvLabel;

        EndViewHolder(View itemView) {
            super(itemView);
            this.tvLabel = itemView.findViewById(R.id.tv_end_label);
            this.tvLocation = itemView.findViewById(R.id.tv_end_location);
        }

        void bind(Itinerary.EndStep step) {
            this.tvLocation.setText(step.getLocation());
            this.tvLabel.setText(step.getLabel());
        }

    }


}
