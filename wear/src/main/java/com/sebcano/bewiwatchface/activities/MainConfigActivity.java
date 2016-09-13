package com.sebcano.bewiwatchface.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sebcano.bewiwatchface.R;
import com.sebcano.bewiwatchface.apiwrap.OptionsStorage;

public class MainConfigActivity extends Activity implements
        WearableListView.ClickListener, WearableListView.OnScrollListener {
    private static final String TAG = "MainConfigActivity";

    private TextView mHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main_config);

        mHeader = (TextView) findViewById(R.id.header);

        BoxInsetLayout content = (BoxInsetLayout) findViewById(R.id.content);
        // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets( View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative( getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }
                return v.onApplyWindowInsets(insets);
            }
        });

        WearableListView listView = (WearableListView) findViewById(R.id.option_picker);
        listView.setHasFixedSize(true);
        listView.setClickListener(this);
        listView.addOnScrollListener(this);
        listView.setAdapter( new OptionsListAdapter() );
    }


    @Override // WearableListView.ClickListener
    public void onClick(WearableListView.ViewHolder viewHolder) {
        OptionItemViewHolder optionHolder = (OptionItemViewHolder) viewHolder;
        optionHolder.mOptionItem.onClick();
        //ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) viewHolder;
        //updateConfigDataItem(colorItemViewHolder.mColorItem.getColor());
        //finish();
    }

    @Override // WearableListView.ClickListener
    public void onTopEmptyRegionClick() {}

    @Override // WearableListView.OnScrollListener
    public void onScroll(int scroll) {}

    @Override // WearableListView.OnScrollListener
    public void onAbsoluteScrollChange(int scroll) {
        float newTranslation = Math.min(-scroll, 0);
        mHeader.setTranslationY(newTranslation);
    }

    @Override // WearableListView.OnScrollListener
    public void onScrollStateChanged(int scrollState) {}

    @Override // WearableListView.OnScrollListener
    public void onCentralPositionChanged(int centralPosition) {}

    private class ToggleListOption {
        private int mIconId;
        private String mTitle;
        private int mKeyId;
        private String[] mLabels;
        private String[] mValues;
        private int mSelected = 0;


        public ToggleListOption( int iconId, String title, int keyId, String [] optionsLabels, String [] optionsValues ) {
            mIconId = iconId;
            mTitle = title;
            mKeyId = keyId;
            mLabels = optionsLabels;
            mValues = optionsValues;
        }

        public int getIconId() {
            return mIconId;
        }

        public String getTitle() {
            return mTitle;
        }

        public int getKeyId() {
            return mKeyId;
        }

        public String getCurrentLabel() {
            return mLabels[mSelected];
        }

        public String getCurrentValue() {
            return mValues[mSelected];
        }

        public void setCurrentValue( String currentValue ) {
            for (int i=0; i<mValues.length; i++) {
                if (mValues[i].equals( currentValue )) {
                    mSelected = i;
                    break;
                }
            }
        }

        public void next() {
            mSelected = (mSelected+1)%mLabels.length;
        }
    }

    private static class OptionItemView extends LinearLayout implements WearableListView.OnCenterProximityListener {
        private static final int ANIMATION_DURATION_MS = 150;
        private static final float SHRINK_CIRCLE_RATIO = .75f;

        private static final float SHRINK_LABEL_ALPHA = .5f;
        private static final float EXPAND_LABEL_ALPHA = 1f;

        private final TextView mLabel;
        private final TextView mValue;
        private final CircledImageView mIcon ;

        private final float mExpandCircleRadius;
        private final float mShrinkCircleRadius;

        private final ObjectAnimator mExpandCircleAnimator;
        private final ObjectAnimator mExpandLabelAnimator;
        private final AnimatorSet mExpandAnimator;

        private final ObjectAnimator mShrinkCircleAnimator;
        private final ObjectAnimator mShrinkLabelAnimator;
        private final AnimatorSet mShrinkAnimator;

        private ToggleListOption mOption;

        public OptionItemView(Context context) {
            super(context);
            View.inflate(context, R.layout.toggle_list_option_item, this);

            mLabel = (TextView) findViewById(R.id.label);
            mValue = (TextView) findViewById( R.id.value );
            mIcon = (CircledImageView) findViewById(R.id.icon);

            mExpandCircleRadius = mIcon.getCircleRadius();
            mShrinkCircleRadius = mExpandCircleRadius * SHRINK_CIRCLE_RATIO;

            mShrinkCircleAnimator = ObjectAnimator.ofFloat(mIcon , "circleRadius",
                    mExpandCircleRadius, mShrinkCircleRadius);
            mShrinkLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
                    EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);
            mShrinkAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mShrinkAnimator.playTogether(mShrinkCircleAnimator, mShrinkLabelAnimator);

            mExpandCircleAnimator = ObjectAnimator.ofFloat(mIcon , "circleRadius",
                    mShrinkCircleRadius, mExpandCircleRadius);
            mExpandLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
                    SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);
            mExpandAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mExpandAnimator.playTogether(mExpandCircleAnimator, mExpandLabelAnimator);
        }

        @Override
        public void onCenterPosition(boolean animate) {
            if (animate) {
                mShrinkAnimator.cancel();
                if (!mExpandAnimator.isRunning()) {
                    mExpandCircleAnimator.setFloatValues(mIcon .getCircleRadius(), mExpandCircleRadius);
                    mExpandLabelAnimator.setFloatValues(mLabel.getAlpha(), EXPAND_LABEL_ALPHA);
                    mExpandAnimator.start();
                }
            } else {
                mExpandAnimator.cancel();
                mIcon .setCircleRadius(mExpandCircleRadius);
                mLabel.setAlpha(EXPAND_LABEL_ALPHA);
            }
        }

        @Override
        public void onNonCenterPosition(boolean animate) {
            if (animate) {
                mExpandAnimator.cancel();
                if (!mShrinkAnimator.isRunning()) {
                    mShrinkCircleAnimator.setFloatValues(mIcon .getCircleRadius(), mShrinkCircleRadius);
                    mShrinkLabelAnimator.setFloatValues(mLabel.getAlpha(), SHRINK_LABEL_ALPHA);
                    mShrinkAnimator.start();
                }
            } else {
                mShrinkAnimator.cancel();
                mIcon .setCircleRadius(mShrinkCircleRadius);
                mLabel.setAlpha(SHRINK_LABEL_ALPHA);
            }
        }

        private void setOption(ToggleListOption option) {
            mOption = option;
            mLabel.setText( mOption.getTitle() );
            mIcon.setImageResource( mOption.getIconId() );

            Context context = getContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
            String currentValue = prefs.getString( context.getString( mOption.getKeyId() ), mOption.getCurrentValue() );
            mOption.setCurrentValue( currentValue );

            mValue.setText( mOption.getCurrentLabel() );
        }

        public void onClick() {
            mOption.next();
            mLabel.setText( mOption.getTitle() );
            mValue.setText( mOption.getCurrentLabel() );

            OptionsStorage store = new OptionsStorage( getContext() );
            store.putString( mOption.getKeyId(), mOption.getCurrentValue() );
        }
    }

    private static class OptionItemViewHolder extends WearableListView.ViewHolder {
        private final OptionItemView mOptionItem;

        public OptionItemViewHolder(OptionItemView optionView) {
            super(optionView);
            mOptionItem = optionView;
        }

    }



    private class OptionsListAdapter extends WearableListView.Adapter {
        public OptionsListAdapter() {
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder( ViewGroup parent, int viewType ) {
            return new OptionItemViewHolder( new OptionItemView(parent.getContext() ) );
        }

        @Override
        public void onBindViewHolder( WearableListView.ViewHolder holder, int position ) {
            OptionItemViewHolder optionHolder = (OptionItemViewHolder) holder;
            ToggleListOption option = null;
            switch (position) {
                case 0:
                    option = new ToggleListOption(
                            R.drawable.ic_thermometer_white_24dp,
                            getString( R.string.temperature_unit ),
                            R.string.temperature_key,
                            getResources().getStringArray(R.array.temperature_array),
                            getResources().getStringArray(R.array.temperature_values_array) );
                    break;
                case 1:
                    /*option = new ToggleListOption(
                            getString( R.string.temperature_unit ),
                            getResources().getStringArray(R.array.temperature_array),
                            getResources().getStringArray(R.array.temperature_values_array) );*/
                    break;
            }
            optionHolder.mOptionItem.setOption(option);
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    /*private class ColorListAdapter extends WearableListView.Adapter {
        private final String[] mColors;

        public ColorListAdapter(String[] colors) {
            mColors = colors;
        }

        @Override
        public ColorItemViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {
            return new ColorItemViewHolder(new ColorItem(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) holder;
            String colorName = mColors[position];
            colorItemViewHolder.mColorItem.setColor(colorName);

            RecyclerView.LayoutParams layoutParams =
                    new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            int colorPickerItemMargin = (int) getResources()
                    .getDimension(R.dimen.digital_config_color_picker_item_margin);
            // Add margins to first and last item to make it possible for user to tap on them.
            if (position == 0) {
                layoutParams.setMargins(0, colorPickerItemMargin, 0, 0);
            } else if (position == mColors.length - 1) {
                layoutParams.setMargins(0, 0, 0, colorPickerItemMargin);
            } else {
                layoutParams.setMargins(0, 0, 0, 0);
            }
            colorItemViewHolder.itemView.setLayoutParams(layoutParams);
        }

        @Override
        public int getItemCount() {
            return mColors.length;
        }
    }*/

    /** The layout of a color item including image and label. */
    /*private static class ColorItem extends LinearLayout implements
            WearableListView.OnCenterProximityListener {
        private static final int ANIMATION_DURATION_MS = 150;
        private static final float SHRINK_CIRCLE_RATIO = .75f;

        private static final float SHRINK_LABEL_ALPHA = .5f;
        private static final float EXPAND_LABEL_ALPHA = 1f;

        private final TextView mLabel;
        private final CircledImageView mColor;

        private final float mExpandCircleRadius;
        private final float mShrinkCircleRadius;

        private final ObjectAnimator mExpandCircleAnimator;
        private final ObjectAnimator mExpandLabelAnimator;
        private final AnimatorSet mExpandAnimator;

        private final ObjectAnimator mShrinkCircleAnimator;
        private final ObjectAnimator mShrinkLabelAnimator;
        private final AnimatorSet mShrinkAnimator;

        public ColorItem(Context context) {
            super(context);
            View.inflate(context, R.layout.color_picker_item, this);

            mLabel = (TextView) findViewById(R.id.label);
            mColor = (CircledImageView) findViewById(R.id.color);

            mExpandCircleRadius = mColor.getCircleRadius();
            mShrinkCircleRadius = mExpandCircleRadius * SHRINK_CIRCLE_RATIO;

            mShrinkCircleAnimator = ObjectAnimator.ofFloat(mColor, "circleRadius",
                    mExpandCircleRadius, mShrinkCircleRadius);
            mShrinkLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
                    EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);
            mShrinkAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mShrinkAnimator.playTogether(mShrinkCircleAnimator, mShrinkLabelAnimator);

            mExpandCircleAnimator = ObjectAnimator.ofFloat(mColor, "circleRadius",
                    mShrinkCircleRadius, mExpandCircleRadius);
            mExpandLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
                    SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);
            mExpandAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mExpandAnimator.playTogether(mExpandCircleAnimator, mExpandLabelAnimator);
        }

        @Override
        public void onCenterPosition(boolean animate) {
            if (animate) {
                mShrinkAnimator.cancel();
                if (!mExpandAnimator.isRunning()) {
                    mExpandCircleAnimator.setFloatValues(mColor.getCircleRadius(), mExpandCircleRadius);
                    mExpandLabelAnimator.setFloatValues(mLabel.getAlpha(), EXPAND_LABEL_ALPHA);
                    mExpandAnimator.start();
                }
            } else {
                mExpandAnimator.cancel();
                mColor.setCircleRadius(mExpandCircleRadius);
                mLabel.setAlpha(EXPAND_LABEL_ALPHA);
            }
        }

        @Override
        public void onNonCenterPosition(boolean animate) {
            if (animate) {
                mExpandAnimator.cancel();
                if (!mShrinkAnimator.isRunning()) {
                    mShrinkCircleAnimator.setFloatValues(mColor.getCircleRadius(), mShrinkCircleRadius);
                    mShrinkLabelAnimator.setFloatValues(mLabel.getAlpha(), SHRINK_LABEL_ALPHA);
                    mShrinkAnimator.start();
                }
            } else {
                mShrinkAnimator.cancel();
                mColor.setCircleRadius(mShrinkCircleRadius);
                mLabel.setAlpha(SHRINK_LABEL_ALPHA);
            }
        }

        private void setColor(String colorName) {
            mLabel.setText(colorName);
            mColor.setCircleColor( Color.parseColor(colorName));
        }

        private int getColor() {
            return mColor.getDefaultCircleColor();
        }
    }

    private static class ColorItemViewHolder extends WearableListView.ViewHolder {
        private final ColorItem mColorItem;

        public ColorItemViewHolder(ColorItem colorItem) {
            super(colorItem);
            mColorItem = colorItem;
        }
    }*/
}
