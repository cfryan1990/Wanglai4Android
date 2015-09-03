package com.cfryan.wanglai4android.adapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cfryan.wanglai4android.R;
import com.cfryan.wanglai4android.model.ContactItem;
import com.cfryan.wanglai4android.model.ContactModel;
import com.cfryan.wanglai4android.model.ContactSortModel;
import com.cfryan.wanglai4android.model.RosterModel;
import com.cfryan.wanglai4android.ui.view.PinnedSectionListView;
import com.cfryan.wanglai4android.util.CharacterParser;
import com.cfryan.wanglai4android.util.L;
import com.cfryan.wanglai4android.util.PinyinComparator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;


public class ContactAdapter extends BaseAdapter implements SectionIndexer, PinnedSectionListView.PinnedSectionListAdapter {
    private Context mContext;
    private ContactSortModel mContactSortModel;  //联系人查询模型
    private List<ContactItem> mItems = null;
    private List<ContactModel> mContactList = null;
    private CharacterParser mCharacterParser;
    private String mFilterString;

    private ContactItem[] sections;
    private int sectionSize;

    private int contactsSize;

    @Override
    public void notifyDataSetChanged() {
        mContactList = queryData("friends");
        contactsSize = mContactList.size();
        generateData(mContactList);
        super.notifyDataSetChanged();
    }

    public String getFilterString() {
        return mFilterString;
    }

    public int getContactSize() {
        return contactsSize;
    }

    public void setFilterString(String filterString) {
        this.mFilterString = filterString;
        List<ContactModel> tmp = filterData(filterString, mContactList);
        contactsSize = tmp.size();
        generateData(tmp);
        super.notifyDataSetChanged();
    }

    public ContactAdapter(Context context) {
        mContext = context;
        mContactSortModel = new ContactSortModel(context);

        mItems = new ArrayList<>();

        mCharacterParser = CharacterParser.getInstance();
        mContactList = queryData("friends");
        contactsSize = mContactList.size();
        generateData(mContactList);
    }

    private void generateData(List<ContactModel> conatclist) {
        mItems.clear();
        //section的数量不会超过27,即26个字母+#
        prepareSections(27);
        String sectionLetter = "";
        int sectionPosition = 0, listPosition = 0;
        for (int i = 0; i < conatclist.size(); i++) {
            String currentLetter = conatclist.get(i).getSortLetters();
            if (!currentLetter.equals(sectionLetter)) {
                ContactItem section = new ContactItem(ContactItem.SECTION, currentLetter);
                section.sectionPosition = sectionPosition;
                section.listPosition = listPosition++;
                onSectionAdded(section, sectionPosition);
                mItems.add(section);
                sectionLetter = currentLetter;
                sectionPosition++;
            }
            ContactItem item = new ContactItem(ContactItem.ITEM, currentLetter);
            item.model = conatclist.get(i);
            item.sectionPosition = sectionPosition;
            item.listPosition = listPosition++;
            mItems.add(item);
        }
        sectionSize = sectionPosition;
    }

    public ContactItem[] getSection() {
        return sections;
    }

    public int getSectionSize() {
        return sectionSize;
    }

    private void prepareSections(int sectionsNumber) {
        sections = new ContactItem[sectionsNumber];
    }

    private void onSectionAdded(ContactItem section, int sectionPosition) {
        sections[sectionPosition] = section;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public ContactItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }


    private List<ContactModel> queryData(String groupname) {
        if (mContactList == null) {
            mContactList = new ArrayList<>();
        } else {
            mContactList.clear();
        }

        List<RosterModel> rosters = mContactSortModel.getRosters(groupname);

        for (int i = 0; i < rosters.size(); i++) {
            ContactModel sortModel = new ContactModel();
            sortModel.setRoster(rosters.get(i));
            // 汉字转换成拼音
            String pinyin = mCharacterParser.getSelling(rosters.get(i).getAlias());
            String sortString = pinyin.substring(0, 1).toUpperCase();
            // 正则表达式，判断首字母是否是英文字母
            if (sortString.matches("[A-Z]")) {
                sortModel.setSortLetters(sortString.toUpperCase());
            } else {
                sortModel.setSortLetters("#");
            }
            mContactList.add(sortModel);
        }

        return filterData(mFilterString, mContactList);
    }

    /**
     * 根据输入框中的值来过滤数据并更新ListView
     *
     * @param filterStr
     */
    private List<ContactModel> filterData(String filterStr, List<ContactModel> contactlist) {
        List<ContactModel> filterContactList = new ArrayList<>();
        if (TextUtils.isEmpty(filterStr)) {
            filterContactList = contactlist;
        } else {
            filterContactList.clear();
            L.i(filterStr);
            for (ContactModel sortModel : contactlist) {
                String alias = sortModel.getRoster().getAlias();
                L.i(alias);
                if (alias.contains(filterStr) || mCharacterParser.getSelling(alias).startsWith(filterStr)) {
                    filterContactList.add(sortModel);
                    L.i(sortModel.getRoster().getAlias());
                }
            }

        }

        // 根据a-z进行排序
        Collections.sort(filterContactList, new PinyinComparator());
        return filterContactList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_contact, null);

            viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.tv_contact_name);
            viewHolder.tvImage = (ImageView) convertView.findViewById(R.id.iv_contact_avatar);
            viewHolder.vsDivider = (ViewStub) convertView.findViewById(R.id.viewstub_divider);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        ContactItem item = getItem(position);
        if (item.type == ContactItem.SECTION) {
            convertView.setBackgroundColor(parent.getResources().getColor(R.color.grey_main));
            viewHolder.tvTitle.setText(item.letter);
            viewHolder.tvImage.setVisibility(View.GONE);
        } else {
            convertView.setBackgroundColor(parent.getResources().getColor(R.color.ui_white));
            viewHolder.tvTitle.setText(item.model.getRoster().getAlias());
            viewHolder.tvImage.setVisibility(View.VISIBLE);
            viewHolder.tvImage.setImageResource(R.mipmap.default_mobile_avatar);
            viewHolder.vsDivider.setVisibility(View.GONE);
            if (getCount() - 1 == position || getItem(position + 1).type == ContactItem.SECTION) {
                L.i(getItem(position).model.getRoster().getAlias()+"    divider","full");
                viewHolder.vsDivider.setLayoutResource(R.layout.divider_full);
            } else {
                L.i(getItem(position).model.getRoster().getAlias() + "    divider", "left");
                viewHolder.vsDivider.setLayoutResource(R.layout.divider_margin_left);
            }
            //这里涉及到viewholder复用，因为viewstub不能重复inflate，所以这里只能使用setVisible
            viewHolder.vsDivider.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    @Override
    public boolean isItemViewTypePinned(int viewType) {
        return viewType == ContactItem.SECTION;
    }

    final static class ViewHolder {
        TextView tvTitle;
        ImageView tvImage;
        ViewStub vsDivider;
    }


    /**
     * 根据ListView的当前位置获取分类的首字母的Char ascii值
     */
    @Override
    public int getSectionForPosition(int position) {
//        return mContactList.get(position).getSortLetters().charAt(0);
        if (position >= getCount()) {
            position = getCount() - 1;
        }
        return getItem(position).sectionPosition;
    }

    /**
     * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
     */
    @Override
    public int getPositionForSection(int section) {
//        for (int i = 0; i < getCount(); i++) {
//            String sortWord = mContactList.get(i).getSortLetters();
//            char firstChar = sortWord.toUpperCase().charAt(0);
//            if (firstChar == section) {
//                return i;
//            }
//        }
//
//        return -1;


        if (section >= sections.length) {
            section = sections.length - 1;
        }
        return sections[section].listPosition;
    }


    /**
     * 提取英文的首字母，非英文字母用#代替。
     *
     * @param word
     * @return
     */
    private String getAlpha(String word) {
        String sortWord = word.trim().substring(0, 1).toUpperCase();
        // 正则表达式，判断首字母是否是英文字母
        if (sortWord.matches("[A-Z]")) {
            return sortWord;
        } else {
            return "#";
        }
    }

    @Override
    public ContactItem[] getSections() {
        return sections;
    }

    /**
     * 加载本地图片
     *
     * @param url
     * @return
     */
    public static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis); // /把流转化为Bitmap图片

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
