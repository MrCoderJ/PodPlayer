package com.example.podplayer.ui.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.podplayer.R
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.android.synthetic.main.activity_on_boarding.*
import kotlinx.android.synthetic.main.onboarding_last.view.*

private val FRAGMENTS = arrayOf(R.layout.onboarding_one, R.layout.onboarding_two, R.layout.onboarding_three, R.layout.onboarding_last)
lateinit var tabLayout: TabLayout
class OnBoardingActivity : AppCompatActivity() {
    private var mSectionsPager: SectionsPagerAdapter? = null
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding)

        auth = FirebaseAuth.getInstance()
        //viewPager.adapter = adapter
        // primary section of the activity
        mSectionsPager = SectionsPagerAdapter(supportFragmentManager)

        //setup the viewpager with the sections adapter
        tabLayout = tabs
        container.adapter = mSectionsPager
        tabs.setupWithViewPager(container, true)

       /** val slides = mutableListOf<Slide>().apply {
            add(Slide(R.drawable.podcast_one, ""))
            add(Slide(R.drawable.internet_podcast, ""))
            add(Slide(R.drawable.podcast_one, ""))
        }
        adapter.setSlides(slides)

        TabLayoutMediator(tabs, viewPager) { _, _ -> }.attach()
        startBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
       **/
    }

    override fun onStart() {
        super.onStart()
        val currentUser: FirebaseUser? = auth.currentUser
        updateUI(currentUser)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }


    inner class SectionsPagerAdapter(fm: FragmentManager): FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT){
        override fun getItem(position: Int): Fragment {
            return SectionFragment.newInstance(position)
        }

        override fun getCount(): Int {
            return FRAGMENTS.size
        }

    }
    class SectionFragment: Fragment(){
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            var index: Int? = arguments?.getInt(ARG_SECTION_NUMBER)
            if (index == null){
                index = 0
            }
            val rootView = inflater.inflate(FRAGMENTS[index], container, false)
            if (index == FRAGMENTS.size -1){
                val signin_button = rootView.signin_btn
                val start_btn = rootView.start_btn
                signin_button.setOnClickListener { startActivity(Intent(context, LoginActivity::class.java)) }
                start_btn.setOnClickListener { startActivity(Intent(context, SignupActivity::class.java)) }
            }else{
                val skip_btn = rootView.findViewById<TextView>(R.id.btn_skip)
                skip_btn.setOnClickListener { tabLayout.getTabAt(FRAGMENTS.size -1)?.select() }
            }

            return rootView
        }
        companion object{
            private val ARG_SECTION_NUMBER = "section_number"


            fun newInstance(sectionNumber: Int): SectionFragment{
                val fragment = SectionFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }

    }








    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}