package be.digitalia.fosdem.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.PersonInfoActivity
import be.digitalia.fosdem.adapters.createSimpleItemCallback
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.viewmodels.PersonsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonsListFragment : Fragment(R.layout.recyclerview_fastscroll) {

    private val viewModel: PersonsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PersonsAdapter()
        val holder = RecyclerViewViewHolder(view).apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
            setAdapter(adapter)
            emptyText = getString(R.string.no_data)
            isProgressBarVisible = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow.first { it.refresh !is LoadState.Loading }
            holder.isProgressBarVisible = false
        }

        viewLifecycleOwner.launchAndRepeatOnLifecycle {
            viewModel.persons.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }

    private class PersonsAdapter : PagingDataAdapter<Person, PersonViewHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item_1_material, parent, false)
            return PersonViewHolder(view)
        }

        override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
            val person = getItem(position)
            if (person == null) {
                holder.clear()
            } else {
                holder.bind(person)
            }
        }

        companion object {
            private val DIFF_CALLBACK: DiffUtil.ItemCallback<Person> = createSimpleItemCallback { it.id }
        }
    }

    private class PersonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val textView: TextView = itemView.findViewById(android.R.id.text1)

        var person: Person? = null

        init {
            itemView.setOnClickListener(this)
        }

        fun clear() {
            person = null
            textView.text = null
        }

        fun bind(person: Person) {
            this.person = person
            textView.text = person.name
        }

        override fun onClick(view: View) {
            person?.let {
                val context = view.context
                val intent = Intent(context, PersonInfoActivity::class.java)
                        .putExtra(PersonInfoActivity.EXTRA_PERSON, it)
                context.startActivity(intent)
            }
        }
    }
}