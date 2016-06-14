package com.THLight.USBeacon.Sample.itemFragment;
import com.THLight.USBeacon.Sample.R;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FragmentCat extends Fragment {
    public static final String CAT_COLOR = "cat_color";
    private String color = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        color = this.getArguments().getString(CAT_COLOR);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return initView(inflater, container);
    }
    private View initView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.menu.fragment_cat, container, false);
        TextView txtCat = (TextView) view.findViewById(R.id.txtCat);
        String colorCat = color + " " + txtCat.getText().toString();
        txtCat.setText(colorCat);
        return view;
    }
}