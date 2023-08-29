package com.company.jmixpm.app;

import com.company.jmixpm.entity.Project;
import com.company.jmixpm.entity.ProjectStats;
import com.company.jmixpm.entity.Task;
import io.jmix.core.DataManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProjectStatsService {

    private final DataManager dataManager;

    public ProjectStatsService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public List<ProjectStats> fetchProjectStatistics() {
        List<Project> projects = dataManager.load(Project.class)
                .all()
                .fetchPlan("project-with-tasks")
                .list();

        return projects.stream()
                .map(project -> {
                    ProjectStats stats = dataManager.create(ProjectStats.class);
                    stats.setId(project.getId());
                    stats.setProjectName(project.getName());
                    stats.setTasksCount(project.getTasks().size());

                    Integer estimatedEfforts = project.getTasks().stream()
                            .map(Task::getEstimatedEfforts).reduce(0, Integer::sum);
                    stats.setPlannedEfforts(estimatedEfforts);

                    stats.setActualEfforts(getActualEfforts(project.getId()));

                    return stats;
                }).collect(Collectors.toList());
    }

    public Integer getActualEfforts(UUID projectId) {
        return dataManager.loadValue("select sum(te.timeSpent) from TimeEntry te " +
                        "where te.task.project.id = :projectId", Integer.class)
                .parameter("projectId", projectId)
                .one();
    }
}