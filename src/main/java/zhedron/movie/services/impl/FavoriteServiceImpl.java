package zhedron.movie.services.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import zhedron.movie.dto.response.FavoriteResponse;
import zhedron.movie.dto.response.request.FavoriteRequest;
import zhedron.movie.entity.Favorite;
import zhedron.movie.entity.MediaContent;
import zhedron.movie.entity.User;
import zhedron.movie.exceptions.*;
import zhedron.movie.mappers.FavoriteMapper;
import zhedron.movie.repository.FavoriteRepository;
import zhedron.movie.repository.MediaContentRepository;
import zhedron.movie.services.FavoriteService;
import zhedron.movie.services.UserService;

@Service
public class FavoriteServiceImpl implements FavoriteService {
    private final UserService userService;

    private final FavoriteRepository favoriteRepository;
    private final MediaContentRepository mediaContentRepository;

    private final FavoriteMapper favoriteMapper;

    public FavoriteServiceImpl(UserService userService, FavoriteRepository favoriteRepository, MediaContentRepository mediaContentRepository, FavoriteMapper favoriteMapper) {
        this.userService = userService;
        this.favoriteRepository = favoriteRepository;
        this.mediaContentRepository = mediaContentRepository;
        this.favoriteMapper = favoriteMapper;
    }

    @Override
    @CachePut(value = "favorites", key = "#result.id()")
    public FavoriteResponse create(FavoriteRequest favoriteRequest) {
        User currentUser = userService.getCurrentUser();

        if (favoriteRepository.existsByUserIdAndName(currentUser.getId(), favoriteRequest.getName())) {
            throw new FavoriteExistException("Favorite already exists");
        }

        Favorite favorite = new Favorite();

        favorite.setName(favoriteRequest.getName());
        favorite.setUser(currentUser);

        Favorite savedFavorite = favoriteRepository.save(favorite);

        return favoriteMapper.toFavoriteResponse(savedFavorite);
    }

    @Override
    @Cacheable(value = "favorites", key = "#id")
    public FavoriteResponse update(FavoriteRequest favoriteRequest, long id) {
        User currentUser = userService.getCurrentUser();

        Favorite favoriteFound = findById(id);

        if (!currentUser.getFavorites().contains(favoriteFound)) {
            throw new FavoriteNotFoundException("Favorite not found");
        }

        favoriteFound.setName(favoriteRequest.getName());

        Favorite updatedFavorite = favoriteRepository.save(favoriteFound);

        return favoriteMapper.toFavoriteResponse(updatedFavorite);
    }

    @Override
    @CacheEvict(value = "favorites", key = "#id")
    public void deleteFavorite(long id) {
        User currentUser = userService.getCurrentUser();

        Favorite favorite = favoriteRepository.findById(id).orElseThrow(() -> new FavoriteNotFoundException("Favorite not found with " + id));

        if (!currentUser.getFavorites().contains(favorite)) {
            throw new FavoriteNotFoundException("Favorite not found");
        }

        currentUser.getFavorites().remove(favorite);

        favoriteRepository.delete(favorite);
    }

    @Override
    @Cacheable(value = "favorites", key = "#id")
    public Favorite findById(long id) {
        return favoriteRepository.findById(id).orElseThrow(() -> new FavoriteNotFoundException("Favorite not found with " + id));
    }

    @Override
    @Caching(cacheable = {
            @Cacheable(value = "favorites", key = "#favoriteId"),
            @Cacheable(value = "mediaContents", key = "#mediaId")
    })
    public FavoriteResponse addMediaContentToFavorite(long favoriteId, long mediaId) {
        User currentUser = userService.getCurrentUser();

        Favorite favoriteFound = findById(favoriteId);

        if (!currentUser.getFavorites().contains(favoriteFound)) {
            throw new FavoriteNotFoundException("Favorite not found");
        }

        MediaContent mediaContentFound = mediaContentRepository.findById(mediaId).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with " + mediaId));

        if (favoriteFound.getMediaContents().contains(mediaContentFound)) {
            throw new MediaContentExistInFavoriteException("Media Content exist in favorite");
        }

        favoriteFound.getMediaContents().add(mediaContentFound);

        Favorite addedMediaContentInFavorite = favoriteRepository.save(favoriteFound);

        return favoriteMapper.toFavoriteResponse(addedMediaContentInFavorite);
    }

    @Override
    @Cacheable(value = "mediaContents", key = "#mediaId")
    public FavoriteResponse removeMediaContentFromFavorite(long favoriteId, long mediaId) {
        User currentUser = userService.getCurrentUser();

        Favorite favoriteFound = findById(favoriteId);

        if (!currentUser.getFavorites().contains(favoriteFound)) {
            throw new FavoriteNotFoundException("Favorite not found");
        }

        MediaContent mediaContentFound = mediaContentRepository.findById(mediaId).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with " + mediaId));

        if (!favoriteFound.getMediaContents().contains(mediaContentFound)) {
            throw new MediaContentNotExistInFavoriteException("Media Content not exist in favorite");
        }

        favoriteFound.getMediaContents().remove(mediaContentFound);

        Favorite removedFavorite = favoriteRepository.save(favoriteFound);

        return favoriteMapper.toFavoriteResponse(removedFavorite);
    }

    @Override
    public FavoriteResponse getFavoriteByName(String name) {
        User currentUser = userService.getCurrentUser();

        Favorite favorite = favoriteRepository.findByUserIdAndName(currentUser.getId(), name).orElseThrow(() -> new FavoriteNotFoundException("Favorite not found with " + name));

        return favoriteMapper.toFavoriteResponse(favorite);
    }
}
