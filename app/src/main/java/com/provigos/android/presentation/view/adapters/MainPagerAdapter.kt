package com.provigos.android.presentation.view.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.provigos.android.presentation.view.activities.MainActivity

class MainPagerAdapter(activity: MainActivity?): FragmentStateAdapter(activity!!) {

    private val mFragmentList : MutableList<Fragment> = ArrayList()
    private val mFragmentTitleList: MutableList<String> = ArrayList()

    override fun getItemCount(): Int {
        return mFragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return mFragmentList[position]
    }

    fun addFragment(fragment: Fragment, title: String) {
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    public fun getTabTitle(position: Int): String {
        return mFragmentTitleList[position]
    }

}