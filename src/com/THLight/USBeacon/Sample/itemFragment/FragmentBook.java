package com.THLight.USBeacon.Sample.itemFragment;
import com.THLight.USBeacon.Sample.R;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
public class FragmentBook {
	 
	public class FragmentApple extends Fragment {
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        return initView(inflater, container);
	    }
	    private View initView(LayoutInflater inflater, ViewGroup container) {
	        View view = inflater.inflate(R.menu.fragment_book, container, false);
	        return view;
	    }
	}
}
