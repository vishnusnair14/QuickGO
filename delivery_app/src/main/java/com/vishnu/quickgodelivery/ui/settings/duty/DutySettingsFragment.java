package com.vishnu.quickgodelivery.ui.settings.duty;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.databinding.FragmentDutySettingsBinding;
import com.vishnu.quickgodelivery.miscellaneous.DutySettingsModel;
import com.vishnu.quickgodelivery.server.APIService;
import com.vishnu.quickgodelivery.server.ApiServiceGenerator;

import java.text.MessageFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DutySettingsFragment extends Fragment {

    private final String LOG_TAG = "DutySettingsFragment";
    FragmentDutySettingsBinding binding;
    private Spinner spinnerState;
    private Spinner districtSpinner;
    private Spinner localitySpinner;
    private FirebaseUser user;
    TextView currentStateTextView;
    TextView currentDistrictTextView;
    TextView currentLocalityTextView;
    TextView currentPincodeTextView;

    public DutySettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentDutySettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        spinnerState = binding.stateSpinner;
        districtSpinner = binding.districtSpinner;
        localitySpinner = binding.localitySpinner;

        currentStateTextView = binding.currentStateTextView;
        currentDistrictTextView = binding.currentDistrictTextView;
        currentLocalityTextView = binding.currentLocalityTextView;
        currentPincodeTextView = binding.currentPincodeTextView;

        // Populate state spinner
        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.india_states_array, R.layout.spinner_item);
        stateAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerState.setAdapter(stateAdapter);

        // Set listener for state spinner
        spinnerState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedState = parent.getItemAtPosition(position).toString();
                populateDistrictSpinner(selectedState);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        districtSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDistrict = parent.getItemAtPosition(position).toString();
                populateLocalitySpinner(selectedDistrict);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // Initial population
        populateDistrictSpinner("Select State");
        populateLocalitySpinner("Select Locality");

        binding.refreshDutySettingsDataBtnButton.setOnClickListener(v -> {
            getSavedDutyData();
        });

        binding.updateDutySettingsDataButton.setOnClickListener(v -> {
            if (validateSelections()) {
                updateData();
            }
        });

        getSavedDutyData();
        return root;
    }

    private boolean validateSelections() {
        Object selectedState = spinnerState.getSelectedItem();
        Object selectedDistrict = districtSpinner.getSelectedItem();
        Object selectedLocality = localitySpinner.getSelectedItem();
        String pincode = binding.dutySettingsPincodeEditTet.getText().toString().trim();

        if (selectedState == null || selectedState.toString().equals("Select State")) {
            Toast.makeText(requireContext(), "Please select a state.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedDistrict == null || selectedDistrict.toString().equals("Select District")) {
            Toast.makeText(requireContext(), "Please select a district.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedLocality == null || selectedLocality.toString().equals("Select Locality")) {
            Toast.makeText(requireContext(), "Please select a locality.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (pincode.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a valid pincode.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }


    private void populateDistrictSpinner(String state) {
        int districtsArrayId = switch (state) {
            case "Karnataka" -> R.array.karnataka_districts_array;
            case "Kerala" -> R.array.kerala_districts_array;
            default -> R.array.empty_array
            ;
        };

        ArrayAdapter<CharSequence> districtAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                districtsArrayId,
                R.layout.spinner_item);
        districtAdapter.setDropDownViewResource(R.layout.spinner_item);
        districtSpinner.setAdapter(districtAdapter);
    }

    private void populateLocalitySpinner(String district) {
        int districtsArrayId = switch (district) {
            case "Palakkad" -> R.array.palakkad_locality_array;
            case "Mysuru" -> R.array.mysuru_locality_array;
            default -> R.array.empty_array
            ;
        };

        ArrayAdapter<CharSequence> localityAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                districtsArrayId,
                R.layout.spinner_item);
        localityAdapter.setDropDownViewResource(R.layout.spinner_item);
        localitySpinner.setAdapter(localityAdapter);
    }

    private void updateData() {
        DutySettingsModel dutySettingsModel = new DutySettingsModel(user.getUid(),
                spinnerState.getSelectedItem().toString().toLowerCase(),
                districtSpinner.getSelectedItem().toString().toLowerCase(),
                localitySpinner.getSelectedItem().toString().toLowerCase(),
                binding.dutySettingsPincodeEditTet.getText().toString());

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.updateDutySettingsData(dutySettingsModel);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(requireContext(), response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                } else {

                    Log.e(LOG_TAG, "Response is not successful or body is null.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void getSavedDutyData() {

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.getUserData(user.getUid());

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {

                    JsonObject data = response.body().get("data").getAsJsonObject();
                    currentStateTextView.setText(
                            MessageFormat.format("{0}{1}",
                                    data.get("duty_state").getAsString().substring(0, 1).toUpperCase(),
                                    data.get("duty_state").getAsString().substring(1).toLowerCase())
                    );

                    currentDistrictTextView.setText(
                            MessageFormat.format("{0}{1}",
                                    data.get("duty_district").getAsString().substring(0, 1).toUpperCase(),
                                    data.get("duty_district").getAsString().substring(1).toLowerCase())
                    );

                    currentLocalityTextView.setText(
                            MessageFormat.format("{0}{1}",
                                    data.get("duty_locality").getAsString().substring(0, 1).toUpperCase(),
                                    data.get("duty_locality").getAsString().substring(1).toLowerCase())
                    );

                    currentPincodeTextView.setText(
                            data.get("duty_locality_pincode").getAsString()
                    );
                    Toast.makeText(requireContext(), response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                } else {

                    Log.e(LOG_TAG, "Response is not successful or body is null.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }


}