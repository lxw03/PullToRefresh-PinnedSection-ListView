/*
 * Copyright (C) 2013 Sergej Shafarenka, halfbit.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hb.examples;

import java.util.Locale;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshPinnedSectionListView;
import com.hb.examples.pinnedsection.R;
import com.hb.views.PinnedSectionListView;
import com.hb.views.PinnedSectionListView.PinnedSectionListAdapter;

public class SampleActivity extends Activity implements OnClickListener {

    static class SimpleAdapter extends ArrayAdapter<Item> implements PinnedSectionListAdapter {

        private static final int[] COLORS = new int[] {
            R.color.green_light, R.color.orange_light,
            R.color.blue_light, R.color.red_light };

        public SimpleAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);

            final int sectionsNumber = 'Z' - 'A' + 1;
            prepareSections(sectionsNumber);

            int sectionPosition = 0, listPosition = 0;
            for (char i=0; i<sectionsNumber; i++) {
                Item section = new Item(Item.SECTION, String.valueOf((char)('A' + i)));
                section.sectionPosition = sectionPosition;
                section.listPosition = listPosition++;
                onSectionAdded(section, sectionPosition);
                add(section);

                final int itemsNumber = (int) Math.abs((Math.cos(2f*Math.PI/3f * sectionsNumber / (i+1f)) * 25f));
                for (int j=0;j<itemsNumber;j++) {
                    Item item = new Item(Item.ITEM, section.text.toUpperCase(Locale.ENGLISH) + " - " + j);
                    item.sectionPosition = sectionPosition;
                    item.listPosition = listPosition++;
                    add(item);
                }

                sectionPosition++;
            }
        }

        protected void prepareSections(int sectionsNumber) { }
        protected void onSectionAdded(Item section, int sectionPosition) { }

        @SuppressWarnings("deprecation")
		@Override public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setTextColor(Color.DKGRAY);
            view.setTag("" + position);
            Item item = getItem(position);
            if (item.type == Item.SECTION) {
                view.setBackgroundColor(parent.getResources().getColor(COLORS[item.sectionPosition % COLORS.length]));
			} else {
				WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
				view.setMinHeight((int) ((Math.abs(new Random().nextGaussian()) * wm.getDefaultDisplay().getHeight())*0.333));
			}
            return view;
        }

        @Override public int getViewTypeCount() {
            return 2;
        }

        @Override public int getItemViewType(int position) {
            return getItem(position).type;
        }

        @Override
        public boolean isItemViewTypePinned(int viewType) {
            return viewType == Item.SECTION;
        }

    }

    static class FastScrollAdapter extends SimpleAdapter implements SectionIndexer {

        private Item[] sections;

        public FastScrollAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        @Override protected void prepareSections(int sectionsNumber) {
            sections = new Item[sectionsNumber];
        }

        @Override protected void onSectionAdded(Item section, int sectionPosition) {
            sections[sectionPosition] = section;
        }

        @Override public Item[] getSections() {
            return sections;
        }

        @Override public int getPositionForSection(int section) {
            if (section >= sections.length) {
                section = sections.length - 1;
            }
            return sections[section].listPosition;
        }

        @Override public int getSectionForPosition(int position) {
            if (position >= getCount()) {
                position = getCount() - 1;
            }
            return getItem(position).sectionPosition;
        }

    }

	static class Item {

		public static final int ITEM = 0;
		public static final int SECTION = 1;

		public final int type;
		public final String text;

		public int sectionPosition;
		public int listPosition;

		public Item(int type, String text) {
		    this.type = type;
		    this.text = text;
		}

		@Override public String toString() {
			return text;
		}

	}

	private boolean hasHeaderAndFooter;
	private boolean isFastScroll = true;
	private boolean addPadding;
	private boolean isShadowVisible = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState != null) {
		    isFastScroll = savedInstanceState.getBoolean("isFastScroll");
		    addPadding = savedInstanceState.getBoolean("addPadding");
		    isShadowVisible = savedInstanceState.getBoolean("isShadowVisible");
		    hasHeaderAndFooter = savedInstanceState.getBoolean("hasHeaderAndFooter");
		}
		initializeHeaderAndFooter();
		initializeAdapter();
		initializePadding();
	}

    @Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    outState.putBoolean("isFastScroll", isFastScroll);
	    outState.putBoolean("addPadding", addPadding);
	    outState.putBoolean("isShadowVisible", isShadowVisible);
	    outState.putBoolean("hasHeaderAndFooter", hasHeaderAndFooter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		menu.getItem(0).setChecked(isFastScroll);
		menu.getItem(1).setChecked(addPadding);
		menu.getItem(2).setChecked(isShadowVisible);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
    	    case R.id.action_fastscroll:
    	        isFastScroll = !isFastScroll;
    	        item.setChecked(isFastScroll);
    	        initializeAdapter();
    	        break;
    	    case R.id.action_addpadding:
    	        addPadding = !addPadding;
    	        item.setChecked(addPadding);
    	        initializePadding();
    	        break;
    	    case R.id.action_showShadow:
    	        isShadowVisible = !isShadowVisible;
    	        item.setChecked(isShadowVisible);
    	        ((PinnedSectionListView)getListView()).setShadowVisible(isShadowVisible);
    	        break;
    	    case R.id.action_showHeaderAndFooter:
    	        hasHeaderAndFooter = !hasHeaderAndFooter;
    	        item.setChecked(hasHeaderAndFooter);
    	        initializeHeaderAndFooter();
    	        break;
	    }
	    return true;
	}

	private void initializePadding() {
	    float density = getResources().getDisplayMetrics().density;
	    int padding = addPadding ? (int) (16 * density) : 0;
	    getListView().setPadding(padding, padding, padding, padding);
	}

	private PullToRefreshPinnedSectionListView mpPullToRefreshPinnedSectionListView;
	
	public ListView getListView() {
		if (mpPullToRefreshPinnedSectionListView == null) {
			mpPullToRefreshPinnedSectionListView = (PullToRefreshPinnedSectionListView) findViewById(R.id.list);
			mpPullToRefreshPinnedSectionListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
				@Override
				public void onRefresh(final PullToRefreshBase<ListView> refreshView) {
					String label = DateUtils.formatDateTime(getApplicationContext(), System.currentTimeMillis(),
							DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

					// Update the LastUpdatedLabel
					refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);

					//TODO Do work to refresh the list here.
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							try {
								Thread.sleep(1500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
							SampleActivity.this.runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									// Stop refreshing
									refreshView.onRefreshComplete();									
								}
							});
							
						}
					}).start();
				}
			});
			mpPullToRefreshPinnedSectionListView
					.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1,
								int arg2, long arg3) {
							Item item = (Item) getListView().getAdapter()
									.getItem(arg2);
							if (item != null) {
								Toast.makeText(SampleActivity.this,
										"Item " + arg2 + ": " + item.text,
										Toast.LENGTH_SHORT).show();
							} else {
								Toast.makeText(SampleActivity.this,
										"Item " + arg2, Toast.LENGTH_SHORT)
										.show();
							}

						}
					});
		}
		return mpPullToRefreshPinnedSectionListView.getRefreshableView();
	}
	
	 private void setListAdapter(Object object) {
		 getListView().setAdapter((ListAdapter) object);
		}
	
    private void initializeHeaderAndFooter() {
        setListAdapter(null);
        if (hasHeaderAndFooter) {
        	
        	mpPullToRefreshPinnedSectionListView  = (PullToRefreshPinnedSectionListView) findViewById(R.id.list);
        	
            ListView list = getListView();

            LayoutInflater inflater = LayoutInflater.from(this);
            TextView header1 = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, list, false);
            header1.setText("First header");
            list.addHeaderView(header1);

            TextView header2 = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, list, false);
            header2.setText("Second header");
            list.addHeaderView(header2);

            TextView footer = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, list, false);
            footer.setText("Single footer");
            list.addFooterView(footer);
        }
        initializeAdapter();
    }

	@SuppressLint("NewApi")
    private void initializeAdapter() {
        getListView().setFastScrollEnabled(isFastScroll);
        if (isFastScroll) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                getListView().setFastScrollAlwaysVisible(true);
            }
            setListAdapter(new FastScrollAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1));
        } else {
            setListAdapter(new SimpleAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1));
        }
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Item: " + v.getTag() , Toast.LENGTH_SHORT).show();
    }

}