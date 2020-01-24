package be.digitalia.fosdem.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.PersonInfoActivity
import be.digitalia.fosdem.adapters.createSimpleItemCallback
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.viewmodels.PersonsViewModel

class PersonsListFragment : RecyclerViewFragment() {

    private val adapter = PersonsAdapter()
    private val viewModel: PersonsViewModel by viewModels()

    override fun onCreateRecyclerView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
        return inflater.inflate(R.layout.recyclerview_fastscroll, container, false) as RecyclerView
    }

    override fun onRecyclerViewCreated(recyclerView: RecyclerView, savedInstanceState: Bundle?) {
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setAdapter(adapter)
        emptyText = getString(R.string.no_data)
        isProgressBarVisible = true

        viewModel.persons.observe(viewLifecycleOwner) { persons ->
            adapter.submitList(persons)
            isProgressBarVisible = false
        }
    }

    private class PersonsAdapter : PagedListAdapter<Person, PersonViewHolder>(DIFF_CALLBACK) {

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
            private val DIFF_CALLBACK = createSimpleItemCallback<Person> { oldItem, newItem ->
                oldItem.id == newItem.id
            }
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