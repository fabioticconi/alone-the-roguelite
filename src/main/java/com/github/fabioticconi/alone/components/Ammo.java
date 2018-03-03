/*
 * Copyright (C) 2015-2018 Fabio Ticconi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.fabioticconi.alone.components;

import com.artemis.Component;

/**
 * Author: Fabio Ticconi
 * Date: 02/03/18
 */
public class Ammo extends Component
{
    public String usableBy;

    public Ammo()
    {

    }

    public Ammo(final String usableBy)
    {
        this.usableBy = usableBy;
    }
}
