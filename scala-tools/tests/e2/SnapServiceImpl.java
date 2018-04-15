class SnapServiceImpl {

  interface Person {
    boolean ownsPhoto(Photo photo);
    Person[] getFriends();
  }

  interface Photo {
    boolean isPublicPhoto();
  }

  public boolean isPhotoVisible(Person person, Photo photo) {
        if (person == null) {
            throw new IllegalArgumentException("Person may not be null");
        }
        if (photo == null) {
            throw new IllegalArgumentException("Photo may not be null");
        }
        if (photo.isPublicPhoto()) {
            return true;
        }
        if (person.ownsPhoto(photo)) {
            return true;
        }
        // for (String friendId : person.getFriends()) {
        //     Person friend = this.getPerson(friendId);
        //     if (friend == null || !friend.ownsPhoto(photo)) continue;
        //     return true;
        // }
        Person[] friends = person.getFriends();
        for (int i = 0; i < friends.length; i++) {
          Person friend = friends[i];
          if (friend == null || !friend.ownsPhoto(photo)) continue;
          return true;
        }
        return false;
    }
}
