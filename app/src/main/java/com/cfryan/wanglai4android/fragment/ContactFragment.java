package com.cfryan.wanglai4android.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cfryan.wanglai4android.R;
import com.cfryan.wanglai4android.activity.ChatActivity;
import com.cfryan.wanglai4android.activity.DetailInfoActivity;
import com.cfryan.wanglai4android.adapter.ContactAdapter;
import com.cfryan.wanglai4android.model.ContactItem;
import com.cfryan.wanglai4android.model.ContactSortModel;
import com.cfryan.wanglai4android.ui.view.IndexBar;
import com.cfryan.wanglai4android.ui.view.ClearEditText;

public class ContactFragment extends Fragment {
    public ContactFragment() {

    }

    private TextView footerview;

    private ListView mContactList;
    private IndexBar mIndexBar;
    private TextView mSelectLetterDialog;
    private ContactAdapter mContactAdapter;
    private ClearEditText mFilterEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            // Currently in a layout without a container, so no
            // reason to create our view.
            return null;
        }
        LayoutInflater myInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = myInflater.inflate(R.layout.fragment_contact, container, false);

        initViews(layout);
        return layout;
    }

    public void initViews(View layout) {
        mContactList = (ListView) layout.findViewById(R.id.lv_contact_listview);


        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View newfriendview = (View) inflater.inflate(R.layout.item_contact, null);
        newfriendview.setBackgroundColor(getResources().getColor(R.color.ui_white));
        TextView tvnewfriend = (TextView) newfriendview.findViewById(R.id.tv_contact_name);
        tvnewfriend.setText("新朋友");
        ImageView ivnewfriend = (ImageView) newfriendview.findViewById(R.id.iv_contact_avatar);
        ivnewfriend.setImageResource(R.mipmap.ic_push_friends);

        //动态加载view，viewstub要使用inflate必须已经设置了layout
        ViewStub vsDividerLeft = (ViewStub) newfriendview.findViewById(R.id.viewstub_divider);
        vsDividerLeft.setLayoutResource(R.layout.divider_margin_left);
        vsDividerLeft.inflate();

        View addfriendview = (View) inflater.inflate(R.layout.item_contact, null);
        addfriendview.setBackgroundColor(getResources().getColor(R.color.ui_white));
        TextView addfriends = (TextView) addfriendview.findViewById(R.id.tv_contact_name);
        addfriends.setText("添加朋友");
        ImageView ivaddfriend = (ImageView) addfriendview.findViewById(R.id.iv_contact_avatar);
        ivaddfriend.setImageResource(R.mipmap.ic_add_friends);

        ViewStub vsDividerfull = (ViewStub) addfriendview.findViewById(R.id.viewstub_divider);
        vsDividerfull.setLayoutResource(R.layout.divider_full);
        vsDividerfull.inflate();

        mContactList.addHeaderView(newfriendview);
        mContactList.addHeaderView(addfriendview);

//      mContactList.setFastScrollEnabled(true);

        mContactAdapter = new ContactAdapter(getActivity());

        String[] index = new String[mContactAdapter.getSectionSize()];
        ContactItem[] tmp = mContactAdapter.getSection();
        for (int i = 0; i < mContactAdapter.getSectionSize(); i++) {
            index[i] = tmp[i].letter;
        }

        FrameLayout view = (FrameLayout) layout.findViewById(R.id.fragment_layout);
        mIndexBar = new IndexBar(getActivity(), index);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(50, 30 * index.length);
        layoutParams.gravity = Gravity.CENTER | Gravity.END;
        mIndexBar.setLayoutParams(layoutParams);
        view.addView(mIndexBar);

        // 设置右侧触摸监听
        mIndexBar.setOnTouchingLetterChangedListener(new IndexBar.OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(int index) {
                // 该字母首次出现的位置
                int position = mContactAdapter.getPositionForSection(index);
                if (position != -1) {
                    mContactList.setSelection(position);
                }

            }
        });

        //listview底部设置
        footerview = (TextView) inflater.inflate(R.layout.item_tv_footer, null);
        footerview.setText(mContactAdapter.getContactSize() + "位联系人");
        mContactList.addFooterView(footerview);

        mContactList.setAdapter(mContactAdapter);

        mFilterEditText = (ClearEditText) layout.findViewById(R.id.filter_edit);

        // 根据输入框输入值的改变来过滤搜索
        mFilterEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                // 当输入框里面的值为空，更新为原来的列表，否则为过滤数据列表
                mContactAdapter.setFilterString(s.toString());
                footerview.setText(mContactAdapter.getContactSize() + "位联系人");
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mContactList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // 这里要利用adapter.getItem(position)来获取当前position所对应的对象
               String alias = mContactAdapter.getItem(position).model.getRoster().getAlias();

                Toast.makeText(getActivity(), alias, Toast.LENGTH_SHORT).show();
                startDetailInfoActivity(( mContactAdapter
                                .getItem(position)).model.getRoster().getJid(),
                        alias);
            }
        });

    }

	private void startDetailInfoActivity(String userJid, String alias) {

		Intent detailInfoIntent = new Intent(getActivity(),
				DetailInfoActivity.class);
		Uri userNameUri = Uri.parse(userJid);
		detailInfoIntent.setData(userNameUri);
		detailInfoIntent.putExtra(DetailInfoActivity.INTENT_EXTRA_USERNAME,
				alias);
		startActivity(detailInfoIntent);
	}

	private void startChatActivity(String userJid, String userName) {
		Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
		Uri userNameUri = Uri.parse(userJid);
		chatIntent.setData(userNameUri);
		chatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, userName);
		startActivity(chatIntent);
	}
//
//	private void startPhoneActivity() {
//		Intent intent = new Intent(getActivity(), PhonesActivity.class);
//		startActivity(intent);
//	}
//
//	private void startNewFriendsActivity()
//	{
//		Intent intent = new Intent(getActivity(), NewFriendsActivity.class);
//		startActivity(intent);
//	}
}
