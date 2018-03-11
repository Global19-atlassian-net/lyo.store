package org.eclipse.lyo.store.sync.change;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ChangeHelper is .
 *
 * @author Andrew Berezovskyi (andriib@kth.se)
 * @version $version-stub$
 * @since 0.0.0
 */
public class ChangeHelper {

    public static List<Change> changesCreated(Collection<Change> allChanges) {
        return allChanges.stream()
                .filter(change -> Objects.equals(change.getHistoryResource().getChangeKindEnum(), ChangeKind.CREATION))
                .collect(Collectors.toList());
    }

    public static List<Change> changesModified(Collection<Change> allChanges) {
        return allChanges.stream().filter(
                change -> Objects.equals(change.getHistoryResource().getChangeKindEnum(), ChangeKind.MODIFICATION))
                .collect(Collectors.toList());
    }

    public static List<HistoryResource> historyFrom(Collection<Change> changes) {
        return ChangeHelper.mapFn(changes, Change::getHistoryResource);
    }

    public static <T, R> List<R> mapFn(Collection<T> changes, Function<T, R> mapper) {
        return changes.stream().map(mapper).collect(Collectors.toList());
    }
}
