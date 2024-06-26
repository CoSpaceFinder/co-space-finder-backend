package pwr.cospacefinderbackend.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pwr.cospacefinderbackend.dto.AvailabilityDTO;
import pwr.cospacefinderbackend.dto.SpaceDTO;
import pwr.cospacefinderbackend.exceptions.AlreadyExistsException;
import pwr.cospacefinderbackend.exceptions.NotFoundException;
import pwr.cospacefinderbackend.model.*;
import pwr.cospacefinderbackend.repository.SpaceRepository;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class SpaceService {
    private final SpaceRepository spaceRepository;
    private final AddressService addressService;
    private final UserService userService;
    private final AvailabilityService availabilityService;
    private final ImageService imageService;

    public List<Space> getAllSpaces() {
        List<Space> spaces = spaceRepository.findAll();
        for (Space space : spaces) {
            space.getAvailabilities().sort(Comparator.comparing(Availability::getDayOfWeek));
        }
        return spaceRepository.findAll();
    }

    public Space getSpace(Long id) {
        Space space = spaceRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Space with id " + id + " does not exist")
        );
        space.getAvailabilities().sort(Comparator.comparing(Availability::getDayOfWeek));
        return space;
    }

    public Space addSpace(SpaceDTO space) {
        if (spaceRepository.existsByName(space.getName())) {
            throw new AlreadyExistsException("Space with name " + space.getName() + " already exists");
        }
        Space newSpace = new Space();
        newSpace.setName(space.getName());
        newSpace.setDescription(space.getDescription());
        Address address = addressService.addAddress(space.getAddress());
        newSpace.setAddress(address);
        newSpace.setCapacity(space.getCapacity());
        newSpace.setConveniences(space.getConveniences());
        User owner = userService.getUser(space.getOwnerId());
        newSpace.setOwner(owner);

        newSpace.setAvailabilities(new ArrayList<>());
        checkIfAllAvailabilitiesArePresent(space.getAvailability());
        for (AvailabilityDTO availability : space.getAvailability()) {
            newSpace.getAvailabilities().add(availabilityService.addAvailability(availability));
        }

        newSpace.setImages(new ArrayList<>());

        return spaceRepository.save(newSpace);
    }

    public Space updateSpace(Long id, SpaceDTO updatedSpace) {
        Space space = spaceRepository.findById(id).orElse(null);
        if (space != null) {
            if (spaceRepository.existsByName(updatedSpace.getName()) && !space.getName().equals(updatedSpace.getName())) {
                throw new AlreadyExistsException("Space with name " + updatedSpace.getName() + " already exists");
            }
            space.setName(updatedSpace.getName());
            space.setDescription(updatedSpace.getDescription());
            Address address = addressService.addAddress(updatedSpace.getAddress());
            space.setAddress(address);
            space.setCapacity(updatedSpace.getCapacity());
            space.setConveniences(updatedSpace.getConveniences());
            User owner = userService.getUser(updatedSpace.getOwnerId());
            space.setOwner(owner);

            checkIfAllAvailabilitiesArePresent(updatedSpace.getAvailability());
            updateAvailabilities(space, updatedSpace.getAvailability());

            return spaceRepository.save(space);
        }
        throw new NotFoundException("Space with id " + id + " does not exist");
    }

    private void checkIfAllAvailabilitiesArePresent(List<AvailabilityDTO> availabilities) {
        if (availabilities == null || availabilities.size() != 7) {
            throw new IllegalArgumentException("Availabilities must be present for all days of the week");
        }
        availabilities.sort(Comparator.comparing(AvailabilityDTO::getDayOfWeek));
        for (int i = 0; i < 7; i++) {
            if (availabilities.get(i).getDayOfWeek() != i + 1) {
                throw new IllegalArgumentException("Availabilities must be present for all days of the week");
            }
        }
    }

    private void updateAvailabilities(Space space, List<AvailabilityDTO> availabilities) {
        availabilities.sort(Comparator.comparing(AvailabilityDTO::getDayOfWeek));
        space.getAvailabilities().sort(Comparator.comparing(Availability::getDayOfWeek));

        for (int i = 0; i < availabilities.size(); i++) {
            availabilityService.updateAvailability(space.getAvailabilities().get(i).getId(), availabilities.get(i));
        }
    }

    @Transactional
    public Space deleteSpace(Long id) {
        Space space = spaceRepository.findById(id).orElse(null);
        if (space != null) {
            for (Availability availability : space.getAvailabilities()) {
                availabilityService.deleteAvailability(availability.getId());
            }

            // Clear availabilities list after deleting them
            space.getAvailabilities().clear();

            // Delete the address associated with the space if not null
            Address address = space.getAddress();
            if (address != null) {
                addressService.deleteAddress(address.getId());
                space.setAddress(null);
            }

            // Delete the images associated with the space if not null
            List<Image> images = space.getImages();
            if (images != null) {
                for (Image image : images) {
                    imageService.deleteImage(image.getId());
                }
                space.setImages(null);
            }

            spaceRepository.delete(space);
            return space;
        } else {
            throw new NotFoundException("Space with id " + id + " does not exist");
        }
    }

    public boolean isOpenOnDayOfWeek(Space space, int dayOfWeek) {
        for (Availability availability : space.getAvailabilities()) {
            if (availability.getDayOfWeek() == dayOfWeek) {
                return availability.isOpen();
            }
        }
        return false;
    }

    public int calculateDaysWhenSpaceIsOpen(LocalDate startDate, LocalDate endDate, Space space) {
        int days = 0;
        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            int dayOfWeek = date.getDayOfWeek().getValue();
            if (isOpenOnDayOfWeek(space, dayOfWeek)) {
                days++;
            }
        }
        return days;
    }

    public Image addImage(Long id, MultipartFile image, String caption) throws IOException {
        Space space = spaceRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Space with id " + id + " does not exist")
        );

        if (image == null || caption == null) {
            throw new IllegalArgumentException("Image and caption must be present");
        }

        Image newImage = imageService.addImage(image, caption);
        space.getImages().add(newImage);
        spaceRepository.save(space);
        return newImage;
    }

    public Image deleteImage(Long id, Long imageId) {
        Space space = spaceRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Space with id " + id + " does not exist")
        );

        Image image = imageService.getImage(imageId);
        if (image == null || !space.getImages().contains(image)) {
            throw new NotFoundException("Image with id " + imageId + " does not exist in the space");
        }
        space.getImages().remove(image);
        imageService.deleteImage(imageId);
        spaceRepository.save(space);
        return image;
    }

}
