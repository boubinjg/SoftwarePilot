import numpy as np
import os
import pickle
########################################################################
def cache(cache_path, fn, *args, **kwargs):
    """
    Cache-wrapper for a function or class. If the cache-file exists
    then the data is reloaded and returned, otherwise the function
    is called and the result is saved to cache. The fn-argument can
    also be a class instead, in which case an object-instance is
    created and saved to the cache-file.
    :param cache_path:
        File-path for the cache-file.
    :param fn:
        Function or class to be called.
    :param args:
        Arguments to the function or class-init.
    :param kwargs:
        Keyword arguments to the function or class-init.
    :return:
        The result of calling the function or creating the object-instance.
    """

    # If the cache-file exists.
    if os.path.exists(cache_path):
        # Load the cached data from the file.
        with open(cache_path, mode='rb') as file:
            obj = pickle.load(file)

        print("- Data loaded from cache-file: " + cache_path)
    else:
        # The cache-file does not exist.

        # Call the function / class-init with the supplied arguments.
        obj = fn(*args, **kwargs)

        # Save the data to a cache-file.
        with open(cache_path, mode='wb') as file:
            pickle.dump(obj, file)

        print("- Data saved to cache-file: " + cache_path)

    return obj
########################################################################

def load_cached(cache_path, in_dir):
    """
    Wrapper-function for creating a DataSet-object, which will be
    loaded from a cache-file if it already exists, otherwise a new
    object will be created and saved to the cache-file.
    This is useful if you need to ensure the ordering of the
    filenames is consistent every time you load the data-set,
    :param cache_path:
        File-path for the cache-file.
    :param in_dir:
        Root-dir for the files in the data-set.
        This is an argument for the DataSet-init function.
    :return:
        The DataSet-object.
    """

    print("Creating dataset from the files in: " + in_dir)

    dataset = cache(cache_path=cache_path,
                    fn=DataSet,
                    in_dir=in_dir)

    return dataset

########################################################################

def one_hot_encoded(class_numbers, num_classes=None):
    """
    Generate the One-Hot encoded class-labels from an array of integers.
    For example, if class_number=2 and num_classes=4 then
    the one-hot encoded label is the float array: [0. 0. 1. 0.]
    :param class_numbers:
        Array of integers with class-numbers.
        Assume the integers are from zero to num_classes-1 inclusive.
    :param num_classes:
        Number of classes. If None then use max(class_numbers)+1.
    :return:
        2-dim array of shape: [len(class_numbers), num_classes]
    """

    # Find the number of classes if None is provided.
    # Assumes the lowest class-number is zero.
    if num_classes is None:
        num_classes = np.max(class_numbers) + 1

    return np.eye(num_classes, dtype=float)[class_numbers]


########################################################################


class DataSet:
    def __init__(self, in_dir, exts='.jpg'):
        """
        This code automatically detects how many classes depending the directory structure.
        Please adhere to the following dir-structure:(if "in_dir = Master/") -
        Master/class1/              - Contains all the training images for class 1
        Master/class2/              - Contains all the training images for class 2
        Master/class3/              - Contains all the training images for class 3
        Master/class1/test/         - Contains all the validation images for class 1
        Master/class2/test/         - Contains all the validation images for class 2
        Master/class3/test/         - Contains all the validation images for class 3
        This means there are 3 classes called: class1, class2 and class3.
        The number of folders in "Masters" will correspond to the number of classes
        :param in_dir:
            Root-dir for the files in the data-set.
            This would be 'Master/' in the example above.
        :param exts:
            String or tuple of strings with valid filename-extensions.
            Not case-sensitive.
        :return:
            Object instance.
        """

        # Extend the input directory to the full path.
        in_dir = os.path.abspath(in_dir)

        # Input directory.
        self.in_dir = in_dir

        # Convert all file-extensions to lower-case.
        self.exts = tuple(ext.lower() for ext in exts)

        # Names for the classes.
        self.class_names = []

        # Filenames for all the files in the training-set.
        self.filenames = []

        # Filenames for all the files in the test-set.
        self.filenames_test = []

        # Class-number for each file in the training-set.
        self.class_numbers = []

        # Class-number for each file in the test-set.
        self.class_numbers_test = []

        # Total number of classes in the data-set.
        self.num_classes = 0

        # For all files/dirs in the input directory.
        for name in os.listdir(in_dir):
            # Full path for the file / dir.
            current_dir = os.path.join(in_dir, name)

            # If it is a directory.
            if os.path.isdir(current_dir):
                # Add the dir-name to the list of class-names.
                self.class_names.append(name)

                # Training-set.

                # Get all the valid filenames in the dir (not sub-dirs).
                filenames = self._get_filenames(current_dir)

                # Append them to the list of all filenames for the training-set.
                self.filenames.extend(filenames)

                # The class-number for this class.
                class_number = self.num_classes

                # Create an array of class-numbers.
                class_numbers = [class_number] * len(filenames)

                # Append them to the list of all class-numbers for the training-set.
                self.class_numbers.extend(class_numbers)

                # Test-set

                # Get all the valid filenames in the sub-dir named 'test'.
                filenames_test = self._get_filenames(os.path.join(current_dir, 'test'))

                # Append them to the list of all filenames for the test-set.
                self.filenames_test.extend(filenames_test)

                # Create an array of class-numbers.
                class_numbers = [class_number] * len(filenames_test)

                # Append them to the list of all class-numbers for the test-set.
                self.class_numbers_test.extend(class_numbers)

                # Increase the total number of classes in the data-set.
                self.num_classes += 1
        print("Number of Classes : {}".format(self.num_classes))

    def _get_filenames(self, dir):
        """
        Create and return a list of filenames with matching extensions in the given directory.
        :param dir:
            Directory to scan for files. Sub-dirs are not scanned.
        :return:
            List of filenames. Only filenames. Does not include the directory.
        """

        # Initialize empty list.
        filenames = []

        # If the directory exists.
        if os.path.exists(dir):
            # Get all the filenames with matching extensions.
            for filename in os.listdir(dir):
                if filename.lower().endswith(self.exts):
                    filenames.append(filename)

        return filenames

    def get_paths(self, test=False):
        """
        Get the full paths for the files in the data-set.
        :param test:
            Boolean. Return the paths for the test-set (True) or training-set (False).
        :return:
            Iterator with strings for the path-names.
        """

        if test:
            # Use the filenames and class-numbers for the test-set.
            filenames = self.filenames_test
            class_numbers = self.class_numbers_test

            # Sub-dir for test-set.
            test_dir = "test/"
        else:
            # Use the filenames and class-numbers for the training-set.
            filenames = self.filenames
            class_numbers = self.class_numbers

            # Don't use a sub-dir for test-set.
            test_dir = ""

        for filename, cls in zip(filenames, class_numbers):
            # Full path-name for the file.
            path = os.path.join(self.in_dir, self.class_names[cls], test_dir, filename)

            yield path

    def get_training_set(self):
        """
        Return the list of paths for the files in the training-set,
        and the list of class-numbers as integers,
        and the class-numbers as one-hot encoded arrays.
        """

        return list(self.get_paths()), \
               np.asarray(self.class_numbers), \
               one_hot_encoded(class_numbers=self.class_numbers,
                               num_classes=self.num_classes)

    def get_test_set(self):
        """
        Return the list of paths for the files in the test-set,
        and the list of class-numbers as integers,
        and the class-numbers as one-hot encoded arrays.
        """

        return list(self.get_paths(test=True)), \
               np.asarray(self.class_numbers_test), \
               one_hot_encoded(class_numbers=self.class_numbers_test,
                               num_classes=self.num_classes)


