package inmethod.gitnotetaking.view;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import inmethod.gitnotetaking.MyApplication;
import inmethod.gitnotetaking.R;

public class FileExplorerListAdapter extends BaseAdapter {
    private List<String> m_item;
    private List<String> m_path;
    public ArrayList<Integer> m_selectedItem;
    Context m_context;
    Boolean m_isRoot;
    public FileExplorerListAdapter(Context p_context,List<String> p_item, List<String> p_path,Boolean p_isRoot) {
        m_context=p_context;
        m_item=p_item;
        m_path=p_path;
        m_selectedItem=new ArrayList<Integer>();
        m_isRoot=p_isRoot;
    }

    @Override
    public int getCount() {
        return m_item.size();
    }

    @Override
    public Object getItem(int position) {
        return m_item.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int p_position, View p_convertView, ViewGroup p_parent)
    {
        View m_view = null;
        FileExplorerViewHolder m_viewHolder = null;
        if (p_convertView == null)
        {
            LayoutInflater m_inflater = LayoutInflater.from(m_context);
            m_view = m_inflater.inflate(R.layout.activity_file_explorer_content, null);
            m_viewHolder = new FileExplorerViewHolder();
            m_viewHolder.m_tvFileName = (TextView) m_view.findViewById(R.id.lr_tvFileName);
            m_viewHolder.m_tvFileName.setTextSize( Integer.parseInt( PreferenceManager.getDefaultSharedPreferences(MyApplication.getAppContext()).getString("GitEditTextSize" ,"14" )));
            m_viewHolder.m_tvDate = (TextView) m_view.findViewById(R.id.lr_tvdate);
            m_viewHolder.m_ivIcon = (ImageView) m_view.findViewById(R.id.lr_ivFileIcon);
            m_viewHolder.m_cbCheck = (CheckBox) m_view.findViewById(R.id.lr_cbCheck);
            m_view.setTag(m_viewHolder);
        }
        else
        {
            m_view = p_convertView;
            m_viewHolder = ((FileExplorerViewHolder) m_view.getTag());
        }
        if(!m_isRoot && p_position == 0)
        {
            m_viewHolder.m_cbCheck.setVisibility(View.INVISIBLE);
        }

        if( m_item.get(p_position).indexOf(  MyApplication.getAppContext().getString(R.string.search_mode)  )!=-1 ){
            m_viewHolder.m_tvFileName.setText(m_item.get(p_position));
            m_viewHolder.m_tvFileName.setTextColor(Color.RED);
            m_viewHolder.m_ivIcon.setVisibility(View.INVISIBLE);
            m_viewHolder.m_cbCheck.setVisibility(View.INVISIBLE);
            m_viewHolder.m_tvDate.setVisibility(View.INVISIBLE);
        }else {
            m_viewHolder.m_tvFileName.setText(m_item.get(p_position));
            m_viewHolder.m_tvFileName.setTextColor(androidx.core.content.ContextCompat.getColor(m_context, R.color.text_primary));
            m_viewHolder.m_tvDate.setText(getLastDate(p_position));
            m_viewHolder.m_tvDate.setTextColor(androidx.core.content.ContextCompat.getColor(m_context, R.color.text_secondary));
            m_viewHolder.m_ivIcon.setImageResource(setFileImageType(new File(m_path.get(p_position))));
            m_viewHolder.m_ivIcon.setVisibility(View.VISIBLE);
            m_viewHolder.m_cbCheck.setVisibility(View.VISIBLE);
            m_viewHolder.m_tvDate.setVisibility(View.VISIBLE);
            m_viewHolder.m_cbCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        m_selectedItem.add(p_position);
                    } else {
                        m_selectedItem.remove(m_selectedItem.indexOf(p_position));
                    }
                }
            });
        }
        return m_view;
    }


    private int setFileImageType(File m_file)
    {
        int m_lastIndex=m_file.getAbsolutePath().lastIndexOf(".");
        String m_filepath=m_file.getAbsolutePath();
        if (m_file.isDirectory())
            return R.mipmap.folderopened_yellow;
        else
        {
            if(m_lastIndex==-1){
                return R.mipmap.unknown;
            }else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".pdf"))
            {
                return R.mipmap.pdf;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".txt"))
            {
                return R.mipmap.txt;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".png"))
            {
                return R.mipmap.png;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".md"))
            {
                return R.mipmap.markdown;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".zip"))
            {
                return R.mipmap.zip;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".7z"))
            {
                return R.mipmap.zip7;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".doc"))
            {
                return R.mipmap.doc;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".docx"))
            {
                return R.mipmap.doc;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".xls"))
            {
                return R.mipmap.xls;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".xlsx"))
            {
                return R.mipmap.xls;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".ppt"))
            {
                return R.mipmap.ppt;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".pptx"))
            {
                return R.mipmap.ppt;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".java"))
            {
                return R.mipmap.java;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".kt"))
            {
                return R.mipmap.kt;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".xml"))
            {
                return R.mipmap.xml;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".html"))
            {
                return R.mipmap.html;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".apk")) {
                return R.mipmap.apk;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".jpg")) {
                return R.mipmap.jpg;
            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".jpeg"))
            {
                return R.mipmap.jpg;

            }
            else if(m_filepath.substring(m_lastIndex).equalsIgnoreCase(".epub"))
            {
                return R.mipmap.epub;

            } else
            {
                return R.mipmap.unknown;
            }
        }
    }

    String getLastDate(int p_pos)
    {
        File m_file=new File(m_path.get(p_pos));
        SimpleDateFormat m_dateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        return m_dateFormat.format(m_file.lastModified());
    }
}